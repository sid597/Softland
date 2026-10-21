# Round 5: Extended Breakouts B, E, and G–M

## Prompt



<pasted_content id="978c">
this is the exact altitude i wanted .. and when we are in this few-many-few breakout mode ignore any "no walls of text" or brevity rules—do not compress breakouts into short 1-paragraph asides to save length

so keep going in the "many" phase right now:
- expand G, H, I, and J into full deep breakouts at the same depth as A–F (including the actual math and schema migration mechanics in J)
- fully open the 3 unopened branches at the bottom (capability security, biology, economics)
- push breakout B further up the stack past Meaning (how do change/diffs/versions and views/components/ECS fit into the derived levels?) and push breakout E deeper on language towers / reflection and how containment (relation 3) works alongside delegation
</pasted_content id="978c">


---

## Reply (Verbatim)

This continues the many phase. I put B first because every other breakout refers to its levels, then E, then G–J. The three branches that were unopened are now K, L and M. The reply ends with an updated map, and nothing converges.

For the upper levels I read your log entries of 23, 30 and 31 July, so those levels come from your words and not from the code. For the capability breakout I checked what the store already says about locks and permissions.

---

## B, continued: the levels above Meaning

The bottom seven levels were Log, Facts, Live, Layers, Standing, Computation and Meaning. Each is defined by the question it lets the levels above stop asking. Going higher, four more levels appear, and two of them may not separate cleanly.

### 8. Change

**Settles:** *what changed, between which two states, and which state counts?*

Carry-on §2 separates two needs: "Automatic event history and an explicit meaningful version are different needs." The log already gives the first one; this level is the second. Your 31 July rulings fill it in:
- "diff candidates for same master there is just a default one i think even if they dont accepted these are versioned drafts to see how we got here"
- "No dont send to each live instance"
- testing by "ai getting different instances and passing real flow through it"

So Change holds four things:
- **Named versions.** "This layer as of this stamp", written as a fact, like a git tag. A named version always reads the same.
- **Candidates.** A layer that proposes changes to a base. A component can have several, with one default. Each is real and working before it is accepted. Carry-on §5: "Acceptance makes it an adopted component; it does not confer functionality for the first time."
- **Diffs.** A structured difference between two states of the same things. A diff is derived and never stored as truth, so a better differ can recompute it later.
- **Adoption and reversal.** One act makes a candidate the default, and one act undoes it.

**Why Change sits above Meaning.** A diff needs two things that only Meaning gives:
- It needs to know which things are *the same* in both states, which is identity.
- It needs to know *what kind* each thing is, to choose a text diff, a tree diff, a geometry diff, or "run the pipeline again and compare".

Without Meaning, a diff can only say "these facts differ". Carry-on §2 records that failure: "Knowing that a chat touched three files tells me very little about what the work means." Naming a version is lower, since it needs only layers and stamps. So Change splits: naming a state is low, and comparing states is high.

**A consequence for pipelines.** Carry-on §2 wants to diff an agent pipeline: change a prompt and see what changed downstream. That diff means something only because of Computation's promise that the same inputs give the same output. Then a difference in output must come from the changed input. A model call breaks that promise, because the same prompt can give different answers. So a model call must be an outside step whose result is recorded as a fact, and a pipeline diff compares recorded outputs rather than rerunning. A promise at a lower level decides what a higher level can mean.

**Identity is the hard core.** Comparing two states needs a correspondence: this function before is that function after. The 7 September session called it "correspondence across edits", which is CAD's persistent-naming problem. First-maker decision 3 is the same problem for code across commits. It comes back in E (overrides) and K (revocation), so it is one problem in four places.

### 9. Surfaces: views and interaction

**Settles:** *how do I see this at the level that helps me act, and how does my gesture become a change?*

Your 31 July line states the levels principle for this level: "you could very well show me the rama module implementation that is honest truth but will not make any sense because I am not looking at that level of abstraction." A view is right only if it is true *and* at the right level.

A surface has two halves:
- **Get:** facts become something to look at. A view is a record that reads facts through a list of layers.
- **Put:** a gesture becomes acts. decisions.md already makes this half a record: "What turns what arrived into acts (a commit's files, a gesture, a new fact) is a record in a layer".

Together, get and put are a *lens* (J has the math). The lens law PutGet says that what you put through a view is what you then see. That is carry-on §7's "write to Rama and show the committed result", written as a law.

Your 23 July triple belongs here: "What actions to take -- their outputs/effects--over what". So does your fear of "100s of cases of what a click means". The levels suggest that a click is resolved by two walks rather than by a table of cases:
1. Walk containment. Find the innermost thing under the pointer, then go outward through the things that contain it until one of them handles the action. This walk says *where*.
2. Walk delegation. That thing's handler comes from its template. This walk says *how*.

A browser's DOM does the first walk, which is called event bubbling, and prototype languages do the second. E develops both.

Promises:
- What is shown is a function of facts, with no hidden state.
- Everything shown can say which facts it came from (carry-on §3, "Context should follow attention").
- An edit through a view lands as acts, and the view shows the committed result.

### 10. Components (the ECS layer)

**Settles:** *what is this thing made of, how is it made, and how do I make another one, or a variant?*

Your own words define this level:
- 2 September: "Anything above the waist is stored as data, is collaborative editable, createable ... should be creatable and then saved for reuse or build higher order things from it".
- 30 July: "the actual structure of the component — what parts it contains, how those parts compose, where they go, and how the whole thing is built — is still mostly code."
- The workshop loop: "type/anatomy exists as material → open it → see its composition → edit parts/nesting/bindings/defaults → render the candidate through the real interpreter → activate or reverse it".

Words: entity, component (data on an entity), template (the editable composition behind a live thing), instance, override, variant, part, binding, and system (behaviour that runs over entities with certain components). Behaviour belongs to this level: "the behavior is a very very key part".

Promises:
- An instance shows its template's composition plus its own overrides. This is delegation.
- A part is itself a component, to any depth. This is containment.
- A template change reaches an instance only through a version that the instance follows. Your 31 July "No dont send to each live instance" puts this level on top of Change.

**The discomfort with "type" goes away here.** Carry-on §5 records that the word "type" became uncomfortable. An ECS has no types, only entities with components. What we called a type is just another entity that instances delegate to, a template. Your 23 July line says the same in your words: "both the meta and instance layer are still an instance of another meta layer which will emerge". That is a chain of delegation, not a class hierarchy.

**Finding: Surfaces and Components may be one level.** A view is a component (a layout of parts) with a system (rendering). A component is shown by views. Interaction handlers are parts of components. ECS itself treats rendering as one system among many. The derivation cannot separate levels 9 and 10 cleanly. Either they are one level, which is what your word "ECS layer" says, or the seam is somewhere I haven't found. I'd keep both readings open.

### 11. Work

**Settles:** *what are we doing, why, what is settled, and what is next?*

Words: question, claim, evidence, source (the discourse-graph base named in CLAUDE.md), trail, briefing, decision, working, settled.

Sources: carry-on §2 (the shape and memory of work), §8 (the reasoning map, the working layer, the accepted base), and §10 ("An exploratory thought is not a decision").

Promises:
- Every claim can show its evidence and sources.
- Every decision can show what it rests on. Your 16 October format of premises and conjectures is a Work-level form.
- Tentative and settled things look visibly different.

This level uses every level below it: Change (what changed), Surfaces (trails and briefings), Components (the workspace), Meaning (kinds such as question and claim), Standing (who said it), and Layers (the working layer and the accepted base).

### The full derived stack

```
 11  Work          what are we doing, why, what is settled?
 10  Components ┐  what is it made of; how do I make a variant?
  9  Surfaces   ┘  how do I see it and act on it?        (maybe one level: "ECS")
  8  Change        what changed, and which state counts?
  7  Meaning       what is this, what does it mean?      (key meaning may sit lower)
  6  Computation   how is one thing made from another?
 ═════════════════════ the waist runs through 6: the executor is code, records are data
  5  Standing      who did this, and were they allowed?
  4  Layers        which world am I in?
  3  Live          when it changes, how do I learn?
  2  Facts         what is true, now and then?
  1  Log           did it happen, and in what order?
 ─────────────────────
     Rama · JVM · disks

 threads through every level:  who · when · why · about whom
```

The material axis sits beside this stack, and the vision already has it, in your 30 July words: "three strata — material, Softland code, and host floor". Material holds the cases of every level. Softland code holds the engines of levels 1–6. The host floor is under level 1. The two axes cross, and neither one replaces the other.

**Finding: "base" lives on three levels.**
- "Base" is a layer that others build on (level 4).
- It is the system's layer that everyone reads (level 5). The store has an open question about whether the "world group" is the base.
- It is what has been accepted (level 11). Carry-on §8: "The accepted base is meant to rest on proof, experiments, or reasoning that can be re-derived and checked."

"System" spans levels 4, 5 and 6 in the same way. On 15 October you stopped the sign-in work with "the thing to start from is answering and understaindg what i mean by base, system etc." I can't verify the cause, but it looks like this collision: sign-in needed "base" at level 5, while the vision uses it at level 11.

`★ Insight ─────────────────────────────────────`
- The act's `:subjects` part names whose data an act is about, so that a person can be forgotten later (`src-store/README.md`: "Acts about her carry the same id in :subjects, so forgetting a person meets this"). That is a fourth thread through every level, beside who, when and why: about whom.
- "One lock seals one value" is how an append-only log forgets. Each value is sealed under its own lock, and forgetting destroys the lock. Your ruling put the choice in each layer's hands: "any tool user layer group can decide by themselves what uh, a log delete means for different kind of values for their tool".
`─────────────────────────────────────────────────`

---

## E, continued: towers, reflection, and containment alongside delegation

### Part 1: towers and reflection

**Your words already describe a reflective tower.**
- 23 July: "the meta layer which informs the instances then instances use inform the meta layer further (note both the meta and instance layer are still an instance of another meta layer which will emerge in softland ...)".
- The portal, the same day: "when i ask for the type we open up the portal to a different layer, the component type, and this component type layer has the accumulated record. We are ported to this layer and it is deterministic opening".
- 30 July: "why can't Softland be Smalltalk-like from the start, with the Workshop able to inspect and reshape itself through the same loop, leaving only a minimal VM-like floor beneath it?"

Those lines contain three operations:
- **Reify:** turn how a thing is made into data you can look at. This is the portal.
- **Reflect:** change that data, and the thing runs differently. This is the workshop loop.
- **Absorb:** a reflected change becomes the new floor, and the waist moves down. This is your "trickle down ... bottom out at the code layer".

The history of reflective systems teaches something about each:

1. **The tower can be infinite without being built.** Brian Cantwell Smith's 3-Lisp (1982) gave every program an interpreter, gave that interpreter its own interpreter, and so on without end. A program can reach up and change the level that runs it. Wand and Friedman (1986) showed that this infinite tower can be built, because a level needs to exist only when a program reaches up to it. For Softland, "fractal to the bottom" does not require infinitely many levels to exist. A level becomes real when someone opens the portal to it.

2. **The floor can be written in the language it runs.** Squeak's virtual machine is written in a subset of Smalltalk. Inside the image it runs as a simulator that you can open, step through and debug. For speed, the same source is translated to C. So one source is both material you can open and the floor that runs everything. That makes your "the code editor is Softland at zoom 100" literal: engines could be written so that the land can read and simulate them as records, while they also run compiled.

3. **Towers cost, and the cost has a known cure.** Each level of interpretation multiplies the work.
   - Yoshihiko Futamura (1971) showed that specializing an interpreter to one program yields a compiled program. Specializing the specializer to the interpreter yields a compiler.
   - Amin and Rompf ("Collapsing Towers of Interpreters", POPL 2018) showed that a tower of many interpreters can be collapsed into one compiled program with no interpretive overhead left.

   decisions.md's "a compiled GPU tier from the same record" is the first step of this. So infinite layers do not mean infinite cost: layers on layers can be compiled through and run at the speed of one.

4. **Levels must be kept apart in time, not only in space.** Racket runs macros at compile time and keeps these "phases" strictly separate, so nothing a macro computes for itself leaks into the running program (Flatt, "Composable and Compilable Macros: You Want It When?", 2002). In Softland, a layer that defines words (a grammar, a template) runs at definition time, and things written in those words run at use time. Keeping the two apart is how "a layer's vocabulary never appears in another layer's rows" holds for the fractal.

5. **Knobs are a level's meta-interface, an idea named in 1996.**
   - Gregor Kiczales's "Open Implementation" argued that black-box abstraction fails when the way a module works matters to its user. His answer was two interfaces for every module: a base interface (what it does) and a meta-interface (how it does it, adjustable).
   - The Metaobject Protocol (1991) went further. The language's own rules, such as how objects are made and how calls are dispatched, are themselves objects that you can change.

   Your knobs (F) are that meta-interface. Applied to Softland, the MOP idea means that how delegation resolves (nearest wins, majority, ask the user) can itself be a knob or a record, which is C's "resolution is a view".

6. **Every reflective system has a floor that does not reflect.** In 3-Lisp it is the implementation, in Smalltalk the VM's primitives, in a CPU the silicon. Softland's floor is the act, the log's promises and the executor's totality. Reading the floor is fine. Changing it means versioning it, with a new edition of the act (G develops this).

### Part 2: containment alongside delegation

**Three algebras, not one mechanism.** J has the math; here is the shape.
- **Delegation** (kind-of and built-on) is a lookup that goes up a chain of parents, and the first parent with a value wins. At each place, that is the "first" monoid: take the first defined value, folded along the list. It is associative, so a stack of stacks flattens into one stack, and an empty layer changes nothing.
- **Containment** (made-of) is assembly: a whole is made of parts, and each part is made of parts. Its algebra is an *operad*: a collection of ways to plug parts into a whole, closed under plugging a whole into a part's slot. David Spivak's "operad of wiring diagrams" (2013) is the standard example: boxes with ports, wired together, where any box can be replaced by a whole diagram.
- **Words-for** (one layer gives the words that another is written in) is a translation between vocabularies, which is a functor (J).

Each of the three is recursive, so each is fractal on its own. That is why one word, "layer", felt right: all three are fractal. It is also why one mechanism strains: they are different algebras with different laws.

**How the two interact.** This is where the design work is.

1. **Delegation lifts through containment.** My editor delegates to the vim-like template, and the template contains a keymap, a buffer and a status line. So my editor contains corresponding parts, and each of my parts delegates to the matching part of the template.
   - The ECS library Flecs does exactly this with two relations: `IsA` (delegation) and `ChildOf` (containment). Creating an instance of a prefab that has children also creates child instances. Each child instance is `IsA` its prefab child and `ChildOf` the new parent.
   - Unity's nested prefabs and prefab variants follow the same pattern.
   - So do Figma's main components, nested instances and overrides.

   The law is that delegation applies at each point along containment.

2. **Overrides are addressed by containment paths.** "In my composite, the button's label says Send" is an override at the path composite / button / label. Figma stores nested overrides keyed by the path of ids inside the main component.

3. **The identity problem comes back.** When the template changes its structure (adds a part, removes one, reorders them), which of my overrides still apply? Figma keeps an override when the layer it points at keeps its identity, and drops it when that layer was replaced. That is the identity problem from Change, and from first-maker decision 3. The default in decision 3, that each function has its own number with its name written on it, is what part-level delegation needs too: parts with their own numbers, not positions.

4. **The half-part problem is a choice of grain.** Delegation per place can mix half of vim's keymap with half of vscode's. Delegation per part avoids that, because the whole keymap comes from one parent. Plan 9 chose the coarse grain (H). Once containment is explicit, "per part" is well defined: a part is a containment subtree. Softland could offer both grains as a knob on builds-on: by place or by part.

5. **Multiple parents need a policy.** There are three known ones:
   - Self, the prototype language, reports an ambiguity error when two parents both answer.
   - Python and the store choose by one fixed order.
   - Git has the person merging resolve the conflict once, and records the resolution.

   Error, order or recorded resolution could be a knob on each layer.

6. **Interaction walks both relations, in order.** For a click, walk containment outward from the innermost thing under the pointer until something handles it (where). Then find that thing's handler through delegation from its template (how). Your fear of "100s of cases of what a click means" is answered by structure, not by a table.

7. **Things move between the two relations over time.** Extracting a component into a shared template turns containment into delegation. Inlining a template into one composite turns delegation into containment. Baldwin and Clark call the first move "inverting" (I). Biology made the first move once, at the largest scale: a free-living bacterium became a part inside every cell we are made of (L). A system with provenance can show these moves.

8. **Containment is not a tree.** In 1965 the architect Christopher Alexander wrote "A City is Not a Tree". Planned cities are trees, where each thing belongs to exactly one larger thing. Living cities are semi-lattices, where units overlap. In his example, the newsrack outside a drugstore, the corner's traffic light and the people waiting at it form one unit, and each of those also belongs to other units. The tree-shaped cities fail.
   - Your 13 September image of the codebase was a city: "the architecture docs hierarchy is like the city planning, the the folders are like blocks".
   - Carry-on §2 asks for the semi-lattice: "A block can belong within a message or another structure while remaining independently referenceable and queryable." That is transclusion, one part inside several wholes.

   So containment in Softland is a structure with sharing, not a strict tree. An operad nests parts but does not share them, so it does not cover this on its own. It also opens a question: when a part is shared, are its overrides per whole or global?

**Your editor, seen this way:**
- Your editor `IsA` your-tweaks, which `IsA` vim-like.
- Vim-like contains a keymap (`ChildOf`), and that keymap `IsA` the base keymap.
- Your keymap override sits at the path editor / keymap / Ctrl-S.
- Your command palette is transcluded from vscode-like's, so the same part sits inside two wholes.
- Asking "what is this keymap an instance of?" opens the portal, which is a deterministic walk up the delegation chain.

**Where it pulls:** deep delegation through deep containment makes long lookup chains. That costs time and makes "where did this come from?" hard to answer. Collapsing towers (Part 1) answers the cost, and provenance of reads (H) answers the question. The semi-lattice makes everything harder: a tree has one path to each part, while a semi-lattice has many, so "the path of an override" is no longer unique.

`★ Insight ─────────────────────────────────────`
- The read path's smallest unit is a place, `[:place entity key]`, and nearest-wins works place by place. "Take the whole keymap from vim" needs a part grain that the read path does not have yet.
- First-maker decision 3's default (a function has its own number, with its name as a fact on it) is the identity that four other things need: part-level delegation, override paths, diffs and revocation. One decision serves all of them.
`─────────────────────────────────────────────────`

---

## G. Pace: how a system learns without tearing

**The framing.**
- Stewart Brand's *How Buildings Learn* (1994), building on the architect Frank Duffy, describes a building as six layers that change at different speeds:
  - Site: eternal.
  - Structure: 30 to 300 years.
  - Skin: about 20 years.
  - Services (wiring, plumbing, heating): 7 to 15 years.
  - Space plan (walls and floors): about 3 years in offices, longer in homes.
  - Stuff (furniture): daily.

  The layers "shear", sliding past each other at their own speeds. A building that learns lets them slide. A building that locks them together tears when it changes.
- In 1999 Brand applied the same idea to civilization: fashion, commerce, infrastructure, governance, culture, nature. His summary: "Fast learns, slow remembers. Fast proposes, slow disposes. Fast is discontinuous, slow is continuous. Fast and small instructs slow and big by accrued innovation and by occasional revolution. Slow and big controls small and fast by constraint and constancy. Fast gets all our attention, slow has all the power."
- Gunderson and Holling's *Panarchy* (2002) describes ecosystems as nested cycles of growth, conservation, release and reorganization, running at many scales at once. Two links connect the scales:
  - **Revolt:** a small, fast collapse cascades into a large, slow one, as when a small fire becomes a forest fire because the forest is old and dry.
  - **Remember:** after a collapse, the slow level provides the seeds and memory for reorganization, as the seed bank and the soil do.
- In software:
  - Foote and Yoder's "Big Ball of Mud" (1997) includes a pattern named "Shearing Layers": keep apart the things that change at different rates.
  - Hyrum's law: "With a sufficient number of users of an API, it does not matter what you promise in the contract: all observable behaviors of your system will be depended on by somebody."
  - Rich Hickey's "Spec-ulation" (2016): adding things and requiring less never break anyone. If you must break something, give it a new name.
  - Rust's editions let the language change its rules every few years without breaking old code. Each crate declares its edition, and every edition compiles into one shared lower form, so crates of different editions link together.

**What it shows for Softland:**

1. **The derived levels have paces.** A proposal:

   | Pace | Brand | Softland |
   |---|---|---|
   | forever | site | the act's form; the log's promises (order, atomicity, durability) |
   | decades | structure | what facts, layers and standing mean; the record language |
   | years | skin, governance | the base vocabulary (what keys mean); the permission model; the base's conventions |
   | months | services | capabilities; runners; Rama and its deployment; the executor's internals |
   | weeks | space plan | shared tools that others build on: converters, views, templates |
   | minutes to days | stuff | personal layers; sketches; candidates being worked on |

   The services row carries Brand's classic failure: wiring cast into concrete, so changing the wiring means breaking the structure. Rama is services and the act is site, so the depot name inside every act-id is wiring cast into concrete.

2. **Pace is earned, not assigned.** By Hyrum's law, a layer becomes slow when many things depend on it, whatever its owner intends. The base is slow because everything builds on it. Your personal layer is fast because nothing does. A template becomes slow by becoming popular. So in Softland's fractal, pace emerges from dependents, and it should be visible: how many layers build on this, read it or pin it. That number says how careful a change must be.

3. **Every pace boundary needs three mechanisms.**
   - *Absorb* (fast into slow): adoption and upstreaming. "Fast proposes, slow disposes."
   - *Protect*, in both directions: pinned versions, so a slow change does not break fast layers that didn't ask for it, and a fast experiment does not reach every live instance. This is your "No dont send to each live instance".
   - *See*: provenance and dependents, so each side knows what it touches.

   Missing any one of them gives a known failure:
   - No absorb: the base fossilizes and workarounds pile up above it, which is the big ball of mud.
   - No protect: every change to the base breaks everyone.
   - No see: nobody knows what a change will hit.

4. **Revolt and remember already have places in Softland.**
   - *Revolt:* a widely adopted record proves wrong, its failure cascades into every layer built on its output, and a slow layer has to change. decisions.md already has a formal path for this: "if a record provably cannot say a needed tool twice over, the leaves gain a designed total language ... asked of Sid then."
   - *Remember:* after a collapse, the log is the seed bank. "The store comes out the same when its gates are run again over its two depots" means every view can be rebuilt from the log and replayed through a fixed converter.
   - A layer's life follows the same cycle: new and fast-changing with few dependents; then popular and rigid; then a breaking change or abandonment; then forks and candidates from what remains. A view could show where each layer is in its cycle.

5. **Editions are how the forever layer can still change.** The act already has a `version` part. With one shared lower form (facts) under every version, old acts keep their version forever, new acts use the new one, and layers of different editions still compose. That is the only safe way to change the slowest layer: add an edition over a shared lower form, and never rewrite old acts.

6. **Proof effort should follow pace.** Your "proof based" becomes graded:
   - Things that last forever get proofs, seL4-style, for the floor.
   - Things that last decades get executable models with properties, like the 24 September model.
   - Things that change weekly get checks at adoption.
   - Things that change by the minute get nothing, because "Working hypotheses and raw thoughts need room" (carry-on §8).

7. **Your own words are pace layering.**
   - Absorb: "they will trickle down from top layer to bottom ones and they will bottom out at the code layer" (16 October).
   - Absorb: "maybe it turns out like the other people's becomes the system one on some point" (the store's decisions).
   - Protect: "No dont send to each live instance ... we build test" (31 July).

**Your editor, seen this way:**
- The act is site and the record language is structure.
- The base's keymap vocabulary is skin, and the GPU text renderer is services.
- The shared vim-like template is space plan, and your tweaks are stuff.

Your tweaks can change every minute because nothing builds on them. The vim-like template slows down as people adopt it. The act never changes.

**Where it pulls:** Brand's rates come from materials. Softland's rates come from dependents and pins. A layer can be fast for a dependent who follows its latest version and slow for another who pinned it. So is pace a property of a layer, or of each builds-on edge? Possibly the edge.

---

## H. Plan 9: everyone composes their own world

**The framing.** Plan 9 from Bell Labs (Pike, Thompson, Presotto and others, from the late 1980s) took Unix's "everything is a file" to its end. Every resource is a tree of files served by a file server, and every file server speaks one small protocol, 9P, of about a dozen messages: attach, walk, open, create, read, write, clunk, remove, stat and a few more. The network is a file server (`/net`). So are the window system, the mail system and the debugger's view of a process (`/proc`).

The second idea is the one that matters here: **every process has its own namespace**, its own view of the file tree, built from parts.
- `mount` attaches a file server's tree at a name, and `bind new old` makes one name show another.
- **Union directories:** `bind -b` puts the new tree *before* the existing one, and `bind -a` puts it *after*. A lookup searches the members in order, and the first match wins.
- `-c` marks the member that receives newly created files. So a read searches many members, and a write goes to exactly one.
- A child process inherits its parent's namespace. It can copy it and change the copy (`rfork(RFNAMEG)`), or start clean (`RFCNAMEG`).
- A namespace can be written as a file of bind and mount lines (`/lib/namespace`) and rebuilt from it. Any process's namespace can be read as text (`/proc/n/ns`, the `ns` command).

**What it shows for Softland:**

1. **The store's reads are Plan 9 unions.** A read through a list of layers, nearest first, is a union directory searched in order, and "a view may give another list" is a per-process namespace. The store's reading that "where a read starts and where a person's acts go are two choices of the view" is exactly `-c`: read from many, write to one. Plan 9 found that split necessary 30 years ago.

2. **A namespace is data.** Plan 9's namespace file is a script of binds. The store's equivalent is already decided: "A layer says which layers it builds on, in order, as a fact on the layer." So a list of layers is a fact you can share and version, and sending someone your world means sending them one fact.

3. **"Where did this come from?" needs its own answer.** Plan 9 users ran `ns` to see their namespace, because in a deep union nobody remembered which member a file came from. Softland needs both kinds of answer: every read can say which layer won (where a value came from), and every list of layers can be shown (where a world came from).

4. **Plan 9's unions are shallow, on purpose.** If two members both contain a subdirectory with the same name, the first member's subdirectory is used whole, and the two are not merged inside. That is E's coarse grain: whole parts, never half a keymap. The store's nearest-wins per place is the fine grain. Plan 9 suggests that the coarse grain is the safe default for combining parts from different parents, and the fine grain is right for overriding single values.

5. **Using the same interface at every scale is what makes nesting work.** Plan 9's window system, rio, gives each window its own `/dev/cons` (text), `/dev/mouse` and `/dev/draw` (graphics). A program written for the whole screen runs unchanged inside a window, because the window serves the same files. rio itself runs inside a rio window. For Softland: if a panel offers the same interface as the whole canvas (pointer, keys, a surface to draw on, a list of layers), then any Softland workspace can run inside a panel of another one. Carry-on §5: "The portal itself is another thing made in Softland." Interfaces that repeat at every scale are what make the fractal actually run, and not just look fractal.

6. **Computation can move while the world stays put.** Plan 9's `cpu` command runs your shell on a powerful server and imports your terminal's namespace, so the remote process sees your files and your screen. For Softland, a record can run on rented compute against the asker's list of layers. Carry-on §8: "Expensive calculations can use local resources, remote machines, or rented compute." The work moves, but the world it sees does not.

7. **Time travel was a namespace.** Plan 9's file server took a snapshot of the whole file system every day and served it under `/n/dump/YYYY/MMDD`. To see yesterday, you bind yesterday's tree over today's, and the `yesterday` command shows a file as it was. Later, Venti stored those snapshots as write-once blocks named by their content. For Softland's Change level, a past state is just another layer you can put in your list. Comparing two times means binding two trees, and restoring means binding the past over the present. The store leaves time travel to be "designed later". Plan 9 suggests that design: an as-of state is a layer.

8. **The ideas won, even though the system didn't.** Plan 9 never replaced Unix, but its ideas spread:
   - UTF-8 was designed for it (Thompson and Pike, 1992).
   - Linux's mount namespaces were inspired by its per-process namespaces.
   - Union mounts led to overlayfs.
   - Docker is per-process namespaces plus stacked image *layers*, your word, in the most widely used deployment tool.
   - Windows uses 9P today for file access between Windows and Linux in WSL.

**Your editor, seen this way:** your editor's world is a namespace: vim-like before vscode-like before base, with your tweaks layer marked `-c` so that your changes land there. Opening your editor in a side panel works because the panel serves the same interface as the screen. Seeing yesterday's keymap means binding yesterday's layer.

**Where it pulls:** Plan 9's namespaces belong to one process and live only while it runs. Softland's lists of layers are durable facts with history and permissions. That is more than Plan 9 had, and harder: who may change the list of a shared world? Plan 9 also worked only in whole files, with no fine-grained values, so Softland's per-place unions are finer than anything Plan 9 had to solve.

---

## I. Conway's law and Linux: the social shape of layers

**The framing.**
- Melvin Conway (1968): "organizations which design systems ... are constrained to produce designs which are copies of the communication structures of these organizations." The architecture ends up shaped like who talks to whom.
- It has been measured.
  - Cataldo, Herbsleb and Carley (2008) defined *socio-technical congruence*: the coordination that the code's dependencies require, compared with the coordination that actually happens. Teams with higher congruence finished work faster.
  - Colfer and Baldwin (2016) reviewed the evidence for the "mirroring hypothesis" and found that it holds widely, with one notable exception. Open, collaborative projects can break the mirror through what they call "actionable transparency": everyone can see the code and the discussion and act on them.
- Baldwin and Clark's *Design Rules* (2000) name six things people can do to a modular system:
  - **split** a module;
  - **substitute** one module for another;
  - **augment**, adding a new one;
  - **exclude**, leaving one out;
  - **invert**, pulling a common part down into a shared one;
  - **port**, making one work elsewhere.

  Each creates options, and the value of a modular design is the value of those options.
- Skelton and Pais's *Team Topologies* (2019) designs teams around cognitive load. There are four kinds of team: aligned to a stream of work, a platform team that offers services, enabling teams that help others adopt, and specialist teams for hard subsystems. They interact in three ways: collaboration, X-as-a-service, and facilitating.

**Linux in detail.** Your store decisions already reach for it: "we can like uh, basically draw a line of what we mean by what is being shipped by Linux and what is not being shipped by it", and "other people can use either theirs or the system ones, or maybe it turns out like the other people's becomes the system one on some point".
- **One frozen boundary, with everything inside fluid.** Linus Torvalds's first rule is "we do not break userspace": the system-call interface to programs is sacred. Inside the kernel there is deliberately no stable interface. The kernel document "stable_api_nonsense" explains that internal interfaces change whenever a better design appears, and drivers that want to keep working must live inside the kernel tree, where they are updated with every change.
- **Ownership is a file.** The `MAINTAINERS` file says who owns each part. Changes flow up through those owners, and each one adds a `Signed-off-by:` line, so every change carries the chain of people who passed it along (the Developer Certificate of Origin).
- **A daily candidate base.** `linux-next` merges every maintainer's tree every day, so conflicts show up before the real merge.
- **Distributions are layers.** Debian, Fedora and Arch are each a curated set of packages and policies on the same kernel. They carry "downstream" patches until "upstream" accepts them.
- **Long forks are expensive.** Android's kernel drifted away from mainline Linux. The fight over Android's "wakelocks" began in 2010 and lasted until Linux added its own version in 2012. The industry's answer is "upstream first": don't let a candidate drift from its base.

**What it shows for Softland:**

1. **Linux is the pace split, enforced socially.** One interface is frozen: for Softland, the act and the read play the role of "do not break userspace". Everything inside the store's levels is free to change, as in "stable_api_nonsense". The cost of that freedom is paid by asking anything that depends on the inside to live with it, "in-tree". For Softland, records that use only the act, the read and the record language survive engine changes. Records that reach further must be maintained with the engine, as system layers.

2. **The six operators are what people will do with layers.**
   - Split: break a layer in two.
   - Substitute: point builds-on at another parent.
   - Augment: add a layer.
   - Exclude: drop a part.
   - Invert: pull a common part down into a shared base ("the other people's becomes the system one").
   - Port: make a layer work over another base.

   This gives the Change level a vocabulary: each operator is a kind of change, with its own diff and its own risks.

3. **Conway's law becomes computable in Softland.** Both graphs live in the same store: who talks to whom (conversations, asks, briefings) and what builds on what (builds-on, based-on, the reads of a run). So socio-technical congruence can be a view. Two groups whose layers depend on each other but who never talk are a risk. Two groups who talk all the time but whose layers don't compose are friction. I know of no other system that has both graphs as data in one place.

4. **Softland is built for "actionable transparency".** Everything is in one store and visible, with provenance. By Colfer and Baldwin's finding, that is exactly the condition under which the mirror can break, giving architectures that don't copy the org chart. Or, if you choose, the mirror can be made visible and measured.

5. **Agents are part of the organization.** Your 7 September picture was "10s of agents per person and 100's of people deploying their agents". By Conway, the layers will mirror how the agents are coordinated. If each person's agents talk only to that person, the layers become per-person silos. If agents coordinate across people, shared layers form. Designing how agents coordinate is designing the architecture.

6. **The inverse move.** Teams can be shaped to get the architecture you want; this is called the "inverse Conway" move. In Softland, the mechanisms of layers *are* social design. If forking is cheap and adoption is hard, you get many silos. If adoption is easy and authority flows through groups, you get federations. In Team Topologies terms:
   - the platform team is Softland's system layers;
   - its "X-as-a-service" is the act and the read;
   - its limit on cognitive load is your "less vocabulary" rule, applied to whole groups.

**Your editor, seen this way:** the vim-like template has a maintainer, recorded as a `MAINTAINERS`-style fact. Your tweak to its keymap travels up with the chain of who passed it on. A daily candidate merges everyone's pending editor changes so conflicts show early. Your editor layer is a distribution, and if your fix is good, upstream absorbs it.

**Where it pulls:** Linux has one Linus. Softland's permission model ties every grant to one person, which gives each layer its own Linus. That works for a project and fails for a commons, through bus factor, burnout and capture. Who may act as the system is still open in the store's queue. Linux's answer is a person, and that may not be Softland's answer.

---

## J. The math of translating between vocabularies

**The framing.** David Spivak, whose ologs you cited on 7 September, built a mathematics of databases. In it, a vocabulary is a category, data in it is a functor, and a translation between vocabularies gives exactly three ways to move data. It answers, with theorems, two questions the vision leaves open: how a level's words translate down, and what survives when a vocabulary changes.

### 1. A vocabulary is a category

```
   Line ──inBlock──▶ Block ──inDoc──▶ Doc
     │                                  ▲
     └──────────────lineDoc─────────────┘

   path equation:   lineDoc = inDoc ∘ inBlock
   ("a line's document is its block's document")
```

Objects are kinds of thing. Arrows are keys that point from one kind to another. Equations are rules that two paths agree. Plain values, such as a line's text, are arrows into a type of values, and an extension called "algebraic databases" handles them.

### 2. Data in a vocabulary is a functor into sets

```
   I : C → Set

   I(Line)  = { l1, l2, l3 }     I(inBlock) :  l1 ↦ b1,  l2 ↦ b1,  l3 ↦ b2
   I(Block) = { b1, b2 }         I(inDoc)   :  b1 ↦ d1,  b2 ↦ d1
   I(Doc)   = { d1 }             I(lineDoc) :  l1, l2, l3 ↦ d1     (forced by the equation)
```

For each kind there is a set of things, and for each key a function. "Functor" just means that the equations hold in the data.

### 3. The fact store is the "category of elements"

```
   ∫I :  objects  (c, x)  with  x ∈ I(c)
         arrows   (c, x) ──f──▶ (c', I(f)(x))   for each key  f : c → c'

   π : ∫I → C      (c, x) ↦ c        "each thing's kind"
```

Every arrow of ∫I is one fact: entity x, key f, value I(f)(x). So an entity-key-value store *is* ∫I. The Grothendieck construction makes this exact. Data in vocabulary C corresponds one-to-one with fact graphs that have a kind map π in which every thing of kind c has exactly one value for each key out of c.

The consequence: **flattening typed data into facts loses nothing if, and only if, you keep the vocabulary and each thing's kind.** That is the rigorous form of B's claim that meaning must sit low. Without it, the facts are a graph that no longer says what it was.

### 4. A translation gives three migrations

A translation is a functor F : C → D. It sends each kind to a kind and each key to a path, and keeps the equations. It gives three migrations:

```
   Δ_F : D-data → C-data      Δ_F(J) = J ∘ F
   Σ_F : C-data → D-data      (Σ_F I)(d) = colim of I(c) over all  (c, F(c) → d)
   Π_F : C-data → D-data      (Π_F I)(d) = lim   of I(c) over all  (c, d → F(c))

   Σ_F  ⊣  Δ_F  ⊣  Π_F        (left adjoint, functor, right adjoint)
```

In plain words:
- **Δ is a view.** It reads D's data in C's words: for each kind in C, look at the kind in D that it maps to.
- **Σ pushes forward by gluing.** It makes unions. Where D needs something that nobody supplied, it makes a fresh placeholder for each thing that needs one.
- **Π pushes forward by matching.** It makes joins. Where D needs something, it takes every consistent combination, which may be exactly one or many.

The smallest example. F sends two kinds, Function and Macro, to one kind, Definition:

```
   Σ_F I (Definition) = I(Function) ⊔ I(Macro)       a union
   Π_F I (Definition) = I(Function) × I(Macro)       every pair
   Δ_F J (Function)   = J(Definition) = Δ_F J (Macro)  each old reader sees all definitions
```

### 5. What survives a migration: a checkable answer

Suppose F is **fully faithful**: it embeds the old vocabulary, so no two old kinds are merged and no relationships between old kinds are added or lost. Then:

```
   Δ_F (Σ_F I) ≅ I      and      Δ_F (Π_F I) ≅ I
```

If you migrate forward and read back through the old words, nothing is lost. So the question "is this vocabulary change breaking?" becomes "is there a fully faithful F : Old → New?"
- **Adding** kinds or keys: yes. Accretion is safe.
- **Renaming:** yes. A rename is an isomorphism.
- **Merging** kinds (Function and Macro into Definition): no. F is faithful but not full, and Δ shows old readers the wrong set. You need a query with a filter ("where kind = function"), which is more than a functor.
- **Splitting** a kind: no. No functor sends the old kind anywhere sensible, so a record has to assign each old row to a new kind.

That is Hickey's "accretion, not breakage" as a theorem. It is also a check of the proof-based kind you asked for: a change to a base vocabulary can be tested for safety before it is adopted.

### 6. Worked example: decomposing an atom later

Carry-on §2 asks: "what if something declared atomic later needs to be decomposed? What survives the migration, and what becomes lossy?"

```
   Old:   Block
   New:   Part ──inBlock──▶ Block            F : Old → New, the inclusion (fully faithful)

   Σ_F I (Part) = ∅                  no parts: nothing forces any to exist
   Π_F I (Part) ≅ I(Block)           one part per block: each block is its own single part
   Δ_F (Σ_F I) = Δ_F (Π_F I) = I     the old view survives either way
```

Nothing is lost. The two standard migrations differ only in what they invent for the new level: "parts unknown" (Σ) or "each block is one part" (Π). The real split, into sentences for example, is new information that a converter adds as new facts. The migration never has to pretend to know it.

### 7. Worked example: combining two editor layers

Combining parts from two parents is a **pushout** of vocabularies: you glue them along what they share.

```
   O  (shared):    Binding ──key──▶ Key         Binding ──cmd──▶ Cmd
   A  (vim-like):  O  +  Binding ──mode──▶ Mode
   B  (vscode):    O  +  Binding ──when──▶ When
   P = A +_O B :   O  +  mode  +  when
```

Push vim's data into P along the inclusion A → P:

```
   Σ (When) ≅ I_A(Binding)    each vim binding gets its own unknown "when"
   Π (When) = { * }           all vim bindings share one "when": the default context
```

The Π line holds because nothing in A points out of When, so the limit is taken over nothing, which gives a one-element set. Do the same for vscode's data, then glue the shared keys and commands. Both sets of bindings survive, so where both editors bind Ctrl-S, two binding rows exist side by side. Choosing one of them ("nearest wins") is a separate policy, a query on top.

So the math separates **integration**, which loses nothing and keeps everything, from **resolution**, which is a policy you can change. That matches C: resolution is a view.

### 8. The same pushout merges edits

Samuel Mimram and Cinzia Di Giusto (2013) showed that merging two patches to a file is a pushout in a category of files, once that category is completed so that every pushout exists. In the completed category, a file *with a conflict* is a valid state: a graph of lines rather than a list. The version control system Pijul is built on this. Conflicts are data, not failures, and a later patch resolves them.

For Softland, two candidates of one component merge as a pushout, and their conflicts live as facts until someone resolves them. The same construction merges vocabularies and merges edits.

### 9. Views you can edit through: lenses

Δ is a view. Editing *through* a view is the classic view-update problem (Bancilhon and Spyratos, 1981): one edit to the view has many possible edits to the source. It becomes well defined once you fix a *complement*, the part that must stay unchanged. Lenses (Foster, Pierce and others, 2007) package the two directions, get and put, with two laws:
- GetPut: putting back what you got changes nothing.
- PutGet: what you put is what you then see.

In Softland, get is the view record and put is the gesture converter (B's Surfaces). PutGet is carry-on §7's "write to Rama and show the committed result".

### 10. Live, not bulk

These migrations work on whole data sets at once, but the store is live. Δ is easy to keep live, because it only re-indexes. Σ and Π involve unions and joins, which need incremental view maintenance. Differential dataflow (McSherry and others, 2013) and DBSP (Budiu and others, 2023) show how to update the result of such queries by processing only the changes. Rama's topologies already compute incrementally, so the mechanism exists under the store.

### 11. It has been built

CQL, the Categorical Query Language (Wisnesky, with the company Conexus), implements Δ, Σ and Π with values (the "algebraic databases" of Schultz, Spivak, Vasilakopoulou and Wisnesky, 2017). It has been used for large data-integration jobs.

**Your editor, seen this way:**
- Vim-like and vscode-like are two vocabularies.
- Your editor is their pushout plus your resolution policy.
- Upgrading the editor template is a migration, and whether it is safe is a theorem you can check.
- Your view of the keymap is Δ, and an edit to it is a lens put.

**Where it pulls:**
- The math wants declared vocabularies, a category for each layer, while the store's keys are open and anyone can write any key.
- Work at the level of values, such as joining a block's lines into its text, is outside the pure functor picture. The math supplies the structural skeleton and the safety tests, and records supply the rest.
- The words (functor, Kan extension) are heavy for your "less vocabulary" rule. The engine can hold the math while people see "view", "migrate", "combine" and "is this change safe?".

`★ Insight ─────────────────────────────────────`
- Σ looks along the arrows *into* a kind, so it needs a reverse index ("what points at this?"). The read path already has one: `[:points-at key thing]`. Π looks along the arrows *out of* a kind, which are ordinary reads by entity.
- The store's "nearest layer that holds a fact at a place wins" gives at most one value per place per read. That is section 3's "exactly one value for each key", with "missing" allowed. So the store already has the shape of a category of elements. What it lacks is the C and the π, which are the Meaning level.
`─────────────────────────────────────────────────`

---

## K. Capability security: authority you hold, not authority you are

**The framing.** There are two ways to decide whether an action is allowed.
- **Access lists:** the system asks *who are you?* and checks a list of what that identity may do. Authority is ambient: it goes with your identity everywhere.
- **Capabilities:** you can act on a thing only if you hold a reference that designates it and carries the right to act. Authority is what you hold. To give authority, you hand over a reference. To give less, you hand over a narrowed one.

Dennis and Van Horn named capabilities in 1966. Norm Hardy's "The Confused Deputy" (1988) shows why they matter. A compiler had the right to write a billing file. A user asked it to write its output to the billing file's name, and the compiler, acting with its own authority, overwrote the bill. Access lists cannot prevent this, because the deputy really is allowed to write that file. Capabilities do prevent it, because the user must hand the compiler a reference for the output file, and the user holds none for the billing file.

The tools that grew from this:
- **Least authority:** give each part only what it needs.
- **Attenuation:** wrap a capability in a forwarder that passes on only some operations.
- **Revocation:** grant through a forwarder that you can cut later, called a "caretaker".
- **Membranes:** a wrapper that wraps everything passing through it, so cutting it cuts everything that went through.

Mark Miller's E language and his thesis *Robust Composition* (2006) built these into a programming language. Operating systems built on capabilities include KeyKOS, EROS, seL4 (whose access control is capability-based and which is formally verified) and Fuchsia. WASI, the WebAssembly system interface, gives a module only the directories it is handed.

In distributed systems, capabilities become tokens:
- **Macaroons** (Google, 2014): anyone holding one can add a caveat that narrows it, and nobody can widen it.
- **UCAN** and **Biscuit:** chains of signed delegations back to the owner. Biscuit checks its rules in Datalog.
- **Tahoe-LAFS:** a write capability derives a read capability, which derives a verify capability, never the other way round.

A Google Docs "anyone with the link" is a capability that ordinary people use without thinking about it.

**What Softland has today**, from the store's code and decisions:
- A permission id is `[who layer granted-in parent allows]`: a chain of grants back to a first permission held by a person, each of which can narrow to `:see`. Your rule is that a permission "ties back to a single individual", and whoever holds a permission can give one beneath it.
- **An act names the permission it acts under.** The gate checks that permission. It does not look up everything that `who` may do.
- Locks are for forgetting, not for access: "one lock seals one value", and the lock is destroyed when that value must be forgotten. A layer "whose members hold their own locks", sealed for everyone else, is designed but not built.
- As built, nothing ends a permission. That is a session's reading of the gate, and it was not run. The sign-in work, now reverted, had added "a right can end and lists its keys".

**What it shows:**

1. **Softland is already halfway to capabilities, and it has the better half.** Because an act names its permission, an agent acting for you must say which grant it is using. It cannot fall back on broader authority it happens to have. That is the property that stops confused deputies. What is missing is the rest of the toolkit: narrowing by more than `:see`, ending a grant, and revoking one.

2. **The resident builder needs least authority from its first act.** Picture its grant as a chain of narrowings, in the style of macaroons:
   - act only in *this* working layer;
   - see the base and your layer;
   - write only these kinds of facts;
   - until tonight;
   - with at most this much compute;
   - for this ask.

   Each narrowing is one more caveat on the chain. Today only the layer and `:see` exist.

3. **Revocation is the safety valve for agents.** If an agent goes wrong, you cut its grant, and everything it granted further down, even five levels down, falls with it. The permission chain already has the shape for this, because every child names its parent. It needs a way to end a grant, plus a rule that a grant is valid only while every link above it is valid.

4. **Records should carry no authority of their own.** A converter that the system runs on your behalf must use *your* permission (the one the asker names), never the system's. Otherwise you could ask the system's converter to write into a layer you cannot write, which is exactly the confused deputy. The 16 October session's matcher already said that a run happens "as whom". The capability rule makes that strict: a record is like a WASI module, holding nothing except what the run hands it. That also makes sharing records safe, because running someone else's record cannot leak your other layers to them.

5. **Your encryption question has a capability answer.** On 7 October you asked: "how will encryption work in super nested structure as i discribed above??? who has the key how and where is it stored??" Hierarchical key derivation, as in Tahoe's capabilities or the "HD" keys of BIP32 wallets, follows the same tree as the grants. From a parent key you can derive every child key. In the "hardened" form, a child cannot work back to its parent's key. So you, at the root, can read everything your agents write, while an agent can read only its own branch. The key tree and the grant tree are one tree.

6. **Bearer or bound.** Macaroons and Tahoe capabilities are *bearer*: whoever holds one can use it. That makes them easy to share and dangerous to leak. Softland's grants are *bound* to who, which is safer. But then an agent running on rented compute needs an identity of its own, a key under yours. "Ties back to a single individual" would then mean that every agent's key is a sub-key of a person's key.

7. **One store holds all the grants, so they can be audited.** The usual weakness of capabilities is that "who can see this?" has no central answer, because authority lives in the references people hold. Softland records every grant as a fact, so it can answer that question while still delegating like a capability system. It is a central ledger of decentralized authority.

**Your editor, seen this way:**
- A plugin someone else wrote runs with nothing except what you hand it. It can read your keymap layer, cannot see your notes, and stops working the moment you revoke it.
- An agent tuning your editor gets "act in editor-config only, until tonight".
- When that agent asks a converter to import a vscode theme, the converter writes with the agent's grant, not the system's.

**Where it pulls:**
- People think in "share with Bob", which is a list, rather than "hand Bob a narrowed reference", though shared links show that the reference model can feel natural.
- Revocation costs an extra indirection on every use.
- Bound grants need identity infrastructure for agents (keys, sign-in), which is exactly the work that was reverted on 15 October.

`★ Insight ─────────────────────────────────────`
- The act's `:permission` part is required (an act without it is refused as `:no-permission`), and the gate decides the act's "standing" from that permission, not from everything `who` holds. That is capability-style designation, present since the first act form.
- The permission id nests its parent inside itself, `[who layer granted-in parent allows]`, so the whole chain of delegation travels with every act. A revoked link could be checked by walking that chain, with no separate table.
`─────────────────────────────────────────────────`

---

## L. Biology: levels of organization that evolved to be evolvable

**The framing.** Life is the oldest layered system, and nobody designed it. Molecules, organelles, cells, tissues, organs, organisms, populations and ecosystems each have their own vocabulary and their own regularities, and no level is the privileged one (Denis Noble's "biological relativity", 2012). Biology matters twice for Softland:
- It is an architecture that has already solved evolvability.
- It is the first real domain that the vision names: "a reasonable attack on humans living healthy forever" (carry-on §1). Your 13 September line: "there is a clear bigger usecase of using discourse garphs on the biology usecase because there we have physical objects that we can map directly".

**What it shows:**

1. **The genetic code is a waist that froze.** DNA is read into RNA (transcription), and RNA into protein (translation), through a fixed table that maps 64 three-letter codons onto 20 amino acids and a stop signal. It is a translation between two vocabularies, and it is the same in nearly every living thing. Francis Crick (1968) called it a "frozen accident". The code is universal not because it is the best one, but because once many proteins depended on it, any change would alter all of them at once and kill the organism. The variations that do exist, such as mitochondria reading some codons differently, appear in small lineages with few dependents.

   This is the act form after the first real record, and it is Hyrum's law, three and a half billion years old. A waist freezes because things depend on it, not because someone decrees it. So get it right before it freezes, and give it editions.

2. **The waist is where checking is strongest.** DNA replication proofreads itself, and a separate repair system fixes the mismatches that slip past. The result is about one error per billion bases copied. The genetic machinery, the most universal part, is the most carefully checked. That is proof effort following pace (G): the floor gets the proofs.

3. **Membranes are gates.** A cell membrane lets some things through and not others, and channels and transporters decide which. A signal usually does not enter: a hormone binds a receptor on the outside, and a cascade inside carries the message. The world does not enter the cell; checked messages do. That is the act at the gate. Compartments, the organelles each inside their own membrane with their own chemistry, are layers.

4. **Containment and composition at the deepest level.** Mitochondria were once free-living bacteria that another cell engulfed and kept (Lynn Margulis's endosymbiosis, 1967). They still carry their own DNA and their own variant of the code: a contained part with its own floor. One organism combined two organisms into one. That is the move from E's point 7, at the scale of all complex life.

5. **Horizontal gene transfer is "combine different parts from different ones".** Bacteria pass genes sideways between lineages in small, portable packages called plasmids, and that is how antibiotic resistance spreads so fast. It works only because the waist is shared: a plasmid from one species runs in another because both read the same code with the same machinery. Combination works because the floor is common. Nature shows E's "the fractal has a floor".

6. **One record, many readings: gene expression is knobs.** Every cell in your body has the same genome. A neuron and a liver cell differ in which genes are switched on, set by transcription factors and by epigenetic marks such as methylation and changes to histones. Development is the same record read with different knobs in different places. Hox genes set coordinates along the body, and cells read their coordinates to decide what to become. Richard Dawkins: the genome is a recipe, not a blueprint. decisions.md already says "A recipe is a record". Cell types are layers over one shared record, distinguished by their knob settings (F).

7. **Kirschner and Gerhart's "facilitated variation" is a theory of evolvable architecture.** In *The Plausibility of Life* (2005) they asked why organisms are so evolvable, and answered with four features:
   - **Conserved core processes:** metabolism, the genetic code, the cytoskeleton and signalling pathways, the same across vast lineages. *The floor.*
   - **Weak regulatory linkage:** the core processes are connected by simple signals that are easy to change, so variation needs to change only the signals, not the core. *Knobs and records over the floor.*
   - **Exploratory behaviour:** systems that generate many states and keep the useful ones. Microtubules grow in all directions and are stabilized where they are needed; nerves explore for their targets; the immune system works this way. *Working layers, candidates, agents trying variants: the "many" phase.*
   - **Compartmentation:** different regions use different combinations of the same core. *Layers.*

   Mapped onto the derived stack, this is Softland's design, arrived at by evolution. The vision's aim, that inhabitants can change the tools freely, is the aim of evolvability.

8. **The bow tie.** Csete and Doyle (2004) showed that metabolism is shaped like a bow tie. Many nutrients are broken down into a few universal carriers (ATP, NADH, about a dozen precursor molecules, the amino acids), which are then built up into many products. The knot is the narrow waist (D). Doyle's warning is that such systems are "robust yet fragile". They tolerate the variation they were shaped for, and they are fragile at the knot, a single point that can be hijacked. Viruses use the cell's universal ribosomes precisely because they are universal.

   For Softland, the act and the executor are knots. A flaw there reaches everything, and a malicious record that exploits the executor hijacks the whole land. The knot is where verification and sandboxing must be strongest: A's eBPF verifier, and K's records that carry no authority.

9. **Levels steer each other by goals, not by details.** Michael Levin's work on bioelectricity (his "multiscale competency architecture" and the TAME framework, 2022) treats each level, from molecular networks and cells to tissues and organs, as a problem-solver with goals in its own space. Higher levels steer lower ones by setting their goals, such as a target shape encoded in voltage patterns across tissue, and the cells work out the details. Change the voltage pattern of a flatworm fragment and it grows a second head. Cut that worm again and the two-headed form comes back, with no change to its genes (Durant and others, 2017). For Softland:
   - Each level's engine is an agent keeping its promises.
   - Upper levels steer lower ones through promises and knobs ("decided at once"), not through their internals.
   - Your "10s of agents per person" could be organized this way: a Work-level agent sets goals for Computation-level agents, which set goals for theirs.

10. **Scientific models already compose like this.** SBML, the standard format for models in systems biology, has a "hierarchical model composition" package. A model is built from submodels connected through ports, and an outer model can *replace* an element of a submodel. That is containment plus overriding, in a standard that working biologists already use. If Softland's layers and biology's levels share machinery, then a cell model is a layer, an organ model builds on cell models, and a hypothesis is a working layer over an accepted model. That is carry-on §8's three synced layers, with one mechanism doing both the architecture and the domain.

**Your editor, seen this way:**
- The act and the record language are the conserved core.
- Templates are genes, and your config is gene expression over them.
- Taking vim's keymap into your editor is horizontal transfer, and it works because the waist is shared.
- A plugin ecosystem evolves like bacteria trading plasmids, and spreads good parts and bad parts in the same way.

**Where it pulls:**
- Evolution is wasteful, slow and blind, and it has no undo, while Softland is designed, versioned and reversible. The analogies can flatter.
- Biology has no permissions as such, though the immune system's self-recognition, where cells display fragments of their proteins for inspection, is a kind of identity check.
- The frozen accident cuts both ways: it says a waist will freeze whether or not it is good. Softland has one advantage that life did not have: it can version the waist, and prove it correct before it freezes.

---

## M. Economics: who pays for a layer others build on

**The framing.** Layers that others build on are public goods: using one does not use it up, and it is hard to stop people from using it. Economics predicts that public goods are underfunded. The vision wants the opposite. Carry-on §9 is titled "People build on other people's work", and it lists "gathering, understanding, making knowledge their own, building, simulating, publishing, and selling". It also names the risk: "market legibility could become more rewarding than useful understanding ... The incentive problem remains open." Your 2 October picture: "it's just like uh, how open source works uh, you can write your library and other people can import it".

**What open source learned:**
- **The maintainer burden.** Nadia Eghbal's *Roads and Bridges* (2016) and *Working in Public* (2020) show a few maintainers carrying projects that millions depend on. xkcd 2347 draws all modern digital infrastructure resting on "a project some random person in Nebraska has been thanklessly maintaining since 2003".
- **The failures that taught it:**
  - `left-pad` (2016): an author unpublished eleven lines from npm and broke thousands of builds.
  - `log4shell` (2021): a critical flaw in a volunteer-maintained logging library used everywhere.
  - `colors.js` and `faker.js` (2022): a maintainer sabotaged his own packages in protest at unpaid work.
- **Funding mechanisms that have been tried:**
  - donations (GitHub Sponsors, Open Collective), foundations, paid support, and open core;
  - *quadratic funding* (Buterin, Hitzig and Weyl, 2018), matching money that rewards how many people support something over how much each one gives;
  - *retroactive public-goods funding* (Optimism), on the principle that "it's easier to agree on what was useful than what will be useful";
  - *dependency streaming* (Drips), where money flows automatically to what you depend on;
  - *contribution graphs*: SourceCred computed "cred" over contribution graphs and wound down, and tea.xyz paid by package dependency and was flooded with spam packages.

**What the older thinkers add:**
- **Elinor Ostrom**, *Governing the Commons* (1990): communities sustain shared resources for centuries when eight conditions hold:
  1. clear boundaries;
  2. rules that fit local conditions;
  3. the people affected can change the rules;
  4. monitoring;
  5. graduated sanctions;
  6. cheap conflict resolution;
  7. outsiders recognize the right to organize;
  8. for large systems, **nested enterprises**, meaning governance in layers.

  These read like requirements for shared layers:
  - Boundaries are layers and permissions.
  - Changing the rules means a layer's rules are facts that its members can change.
  - Monitoring is provenance, which the store gives almost for free.
  - Nested enterprises are personal, group and base layers.
- **Ronald Coase** (1937): firms exist because buying through a market costs something: finding, negotiating, enforcing. Softland could make reuse nearly free. Composing is one builds-on fact, attribution is automatic provenance, and payment could be automatic. Then much smaller pieces of work become tradeable. Adam Smith said the division of labour is limited by the extent of the market. Carry-on §9's "Scientific communication can become smaller, more modular, better connected, and more attributable" is the same point in economic terms.
- **Baldwin and Clark** (I): modularity creates option value. A well-layered system is worth more than the sum of its parts, because every part can be improved independently.
- **Herbert Simon** (1971): "a wealth of information creates a poverty of attention". The scarcest resource in Softland is attention, yours included: the walls of text, the five parallel sessions. A layer that others build on costs them attention every time it changes. "Making sense by default" (carry-on §3) is attention economics.
- **Ted Nelson's Xanadu:** every quotation is a live link to its source (transclusion), and every reuse automatically pays its author a tiny royalty ("transcopyright"). It is the purest design ever proposed for the economics of layers, and it was never realized. Nick Szabo's "Micropayments and Mental Transaction Costs" (1999) explains much of why: people dislike deciding about tiny payments, so micropayments work only when nobody has to decide.

**What it shows for Softland:**

1. **Attribution is already computed, and credit can be too.** The store records who wrote each fact, what each act was based on, what caused it, what each layer builds on, and what each run read. That is a contribution graph by construction, at no extra cost. Credit can flow back along it, for example by Shapley values: each contributor's fair share of a joint result is their average marginal contribution. "Data Shapley" (Ghorbani and Zou, 2019) applies this to training data. Your "Contributors should be identifiable through both small and major discoveries" (carry-on §9) becomes a view.

2. **Goodhart's law is the risk the vision already named.** "When a measure becomes a target, it ceases to be a good measure." SourceCred and tea.xyz were gamed as soon as credit became money. James C. Scott's *Seeing Like a State* (1998) is the long version: legibility imposed from above destroys the local knowledge it measures. Carry-on §9's worry, "market legibility could become more rewarding than useful understanding", is exactly this. One defence the stack offers is to pay on *checked* outcomes (claims that passed their checks, G's graded proofs), not on activity. Even that needs checks that measure what matters, and a strong optimizer will find where they don't.

3. **What a fork owes its parent can be a fact on the layer.** Licences are the existing protocol: permissive (keep the attribution), share-alike (forks stay as open as their parent, as with the GPL and CC BY-SA), network share-alike (the AGPL), and non-commercial. In Softland:
   - Attribution is automatic, through provenance.
   - Share-alike is a rule at the Standing level that the gate can check: "a layer built on a share-alike layer may not be narrower in who can see it".
   - Payment is a flow.

   A layer's terms are a knob (F), set by its owner and inherited down builds-on like any other fact.

4. **Commissioned work is a path through the stack.** Carry-on §9: "A lab can ask for a visualization or shader pipeline; someone with filmmaking or scene-building expertise can produce it with their own agents and offer the result to others." The path runs:
   1. an ask (a Work fact, with terms);
   2. candidates (layers from several makers);
   3. checks (claims that the lab states);
   4. adoption (the Change level);
   5. payment on adoption;
   6. the sharing terms of the result.

   Bounties, Kaggle and the XPRIZE have this shape, and Robin Hanson's idea futures are its prediction-market cousin.

5. **Compute already has a meter.** decisions.md: "a record is total with a work budget per read". Ethereum's *gas* is the same design: every operation is metered, a budget bounds every run, and the user pays for what ran. Whether Softland's budgets turn into money, quota or attention, the unit exists. With "10s of agents per person", the question of who pays for runs is not hypothetical.

6. **Data sovereignty is an economic position.** Carry-on §9: personal and business knowledge "need not all go to one external provider", and "Sharing in the broader economy is a user choice." Jaron Lanier and Glen Weyl (2018) proposed "mediators of individual data": groups that negotiate the terms for their members' data collectively. In Softland that is a group layer with terms, holding sealed data (K's member-held locks) and licensing it.

7. **Payment can poison a gift economy.** Richard Titmuss (*The Gift Relationship*, 1970) argued that paying blood donors lowered both supply and quality. Later work on "motivation crowding" (Bruno Frey) found that money can crowd out intrinsic motivation. Open source and science run largely on gifts and reputation. So economics should probably be a choice per layer, a knob: some layers are gifts with attribution, some are commons with Ostrom's rules, some are paid. Which should be the default is open to argument under your 12 October rule (the end that keeps more): attribution keeps more information, and payment keeps more incentive.

**Your editor, seen this way:**
- The vim-like template that thousands build on has a maintainer who carries its weight.
- Your editor layer inherits its terms (share-alike, say) and its attribution automatically.
- Retroactive funding could reward that template because many layers turned out to depend on it.
- A lab that wants a structural-editing mode for its notebooks posts an ask with terms. Three makers send candidates, and the one that passes the lab's checks is adopted and paid. Under the template's share-alike terms, everyone can then build on it.

**Where it pulls:** money needs identity, law and payment rails, all of which sit outside the store, and every metric that becomes money gets gamed. The vision itself keeps this open: "The ownership and commercial arrangement remain a proposal rather than a completed economic design" (carry-on §9).

---

## The map after this round

This is still the many phase, so the map is for finding your way around, not a conclusion.

**Agreements, new and strengthened:**
- **The floor must be common, small, checked hardest, and versioned by editions:**
  - A: the instruction set;
  - D: the waist;
  - E: the floor of reflection;
  - G: forever pace, and editions;
  - J: migration is lossless only if meaning is kept;
  - K: seL4;
  - L: the frozen accident, and proofreading at the waist.
- **Three algebras cover your fractal "layer":** delegation (a monoid), containment (an operad, extended to allow sharing), and translation between vocabularies (a functor). E, H and J arrive at the same three.
- **Identity across change is one problem in many places:** diffs (B), override paths (E), migrations (J), revocation (K), attribution (M), and first-maker decision 3. It probably wants one mechanism: parts with their own numbers.
- **Integration and resolution are separate.** Keep everything from every parent, and choose by a policy on top: C, H and J (the pushout keeps both bindings, and Pijul keeps conflicts as data).
- **Provenance carries much of the load:** H (`ns`), I (`Signed-off-by`), K (grant chains), M (credit) and B (Change).
- **Every pace boundary needs absorb, protect and see:** G, I (upstream first, linux-next), L (facilitated variation) and M (Ostrom's commons rules).

**New tensions:**
1. Is pace a property of a layer, or of each builds-on edge? (G)
2. What is the grain of a union, per place or per part? Plan 9 chose whole parts (H); the store chose places.
3. Is containment a tree, or a structure with sharing (transclusion)? Alexander says sharing; operads say tree. (E)
4. Are Surfaces and Components one level or two? (B)
5. Are grants bound or bearer? Bound is safer; bearer travels to rented compute. (K)
6. Declared vocabularies or open keys? J's guarantees need schemas, while the store lets anyone write any key.
7. Gift, commons or market, and which is the default? (M)
8. Should the layer system mirror the social graph, or break it? (I)
9. Who is the Linus of a shared layer? Tying every grant to one person pulls against commons governance. (I, M)

**Branches still unopened:**
- **Linguistics:** pidgins and creoles, how two languages merge into a new one. This is the social side of J's pushouts.
- **Cognitive load:** chunking (Miller) and cognitive load theory (Sweller). How many words a level can have before a person stops being able to hold it: the science under your "less vocabulary".
- **Law:** Lessig's "code is law". Architecture as regulation, layers as jurisdictions, and licences as checks the gate runs.
- **CRDTs:** data types that merge without coordination. Their algebra (join-semilattices) is a fourth candidate structure for combining layers, beside the three above.
- **Ecology:** niche construction, where organisms reshape the environment that then selects them. This matches your 23 July "the meta layer which informs the instances then instances use inform the meta layer further".
- **Christopher Alexander's later work:** *A Pattern Language* (patterns as records about design) and *The Nature of Order* (wholes made of centres at many scales).

Pick any branch, new or old, to go deeper on, or tell me when you want to start narrowing. Nothing is written to disk.
