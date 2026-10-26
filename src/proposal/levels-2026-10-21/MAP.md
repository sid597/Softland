# Levels: the map of the many (21 October 2026)

This folder holds one exploration. It starts from Sid's two notebook pages
and ends with a stack of abstraction levels for Softland and many breakouts
around it. The method was few-many-few, in Sid's words (his long prompt
‹bb0ad8a0›): "we do start out in a single point but then there are soo many
breakouts and then later convergence just like the neural network images
few-many-few". The many is done. The convergence, the second "few", happens in
a fresh session, and [STARTER.md](STARTER.md) is its prompt.

This page is the top index: where we started, the derived stack, and the map
(agreements, tensions, open questions, unopened branches). The working is in
the rounds, verbatim. **Nothing here is decided.** Every level, agreement and
tension is a session's position unless it is marked as Sid's.

The session that held the many wrote this page from its own context (Claude
Opus 5.5, effort max). Sid extracted the rounds himself.

## The rounds

| File | What it holds |
|---|---|
| [01-notebook-reading.md](01-notebook-reading.md) | The first reply to the notebook pages: the agent as the resident builder, the builder's "first light" walk and the protocols it needs, computation and dependency, naming, proof first, and two questions to Sid. |
| [02-box-and-levels.md](02-box-and-levels.md) | Sid asks whether the "box" holds and pushes for abstraction levels. The reply: the box holds as one level's face; the store is several levels fused; three causes of the spreading; what a level is, and four tests; a first stack; where the session disagreed; why the window is now. |
| [02b-alt-branch-breakouts.md](02b-alt-branch-breakouts.md) | An alternate branch, titled "Depot Correction & Initial Breakouts". It was sent from round 2's reply in place of round 3's prompt, and the session then went on from round 2 to round 3 without it. **The session that wrote this page does not hold it in its context.** Read it before relying on the depot claim of round 2 and of breakouts A (round 3) and G (round 5). It ends with three questions to Sid (under "Questions put to Sid that are still open"). |
| [03-breakouts-A-to-F.md](03-breakouts-A-to-F.md) | After Sid's few-many-few correction: breakouts A to F in depth, G to J in short, and the first map. |
| [04-first-few-to-many.md](04-first-few-to-many.md) | A branch the writing session does not hold. It was sent from round 3's reply in place of round 5's prompt, and the session then went on from round 3 to round 5 without it. Sid asked where we started (the first few) and how the many fit as one system; the reply arranges both without converging. It already begins the convergence's first step. |
| [05-breakouts-B-E-G-to-M.md](05-breakouts-B-E-G-to-M.md) | B pushed up to levels 8 to 11. E pushed deeper: towers, reflection, containment beside delegation. G to J in depth. Three new branches: K capability security, L biology, M economics. The second map. |
| [06-three-math-bridges.md](06-three-math-bridges.md) | Three bridges: sharing, and one level for surfaces and components (Poly); diffs as spans and collapsing chains of migrations; the coherence laws and Nix overlays. The fan-out stops and the handoff is proposed. |

The breakouts, by letter:

- **A** the computer's levels, each a machine for the one above
- **B** the levels Softland needs, derived from the log up
- **C** the store inside out: one truth, many views
- **D** the hourglass, the narrow waist
- **E** the fractal layer
- **F** knobs across levels
- **G** pace
- **H** Plan 9
- **I** Conway's law and Linux
- **J** the math of translating between vocabularies
- **K** capability security
- **L** biology
- **M** economics

## Where we started (the first "few")

### The notebook pages

Photos: [p1](../../../vision/images/2026-10-21-notebook-p1.png),
[p2](../../../vision/images/2026-10-21-notebook-p2.png). The text below is a
model transcription of Sid's handwriting; the images are the source. Sid
sent them with the line "this is what i have been thinking" (‹8f0e5667›); the
same words went in the same minute to two other sessions (‹4fba4ead›,
‹142d89ee›).

Page 1:

> Problems:
> - How and where to store computations functions.
> - If code is in data how to solve the problem of dependency
> - Code is going to be stored. we can for initial version only store data on server side because that is the interface.
> - So we will populate the server with data from non-browser way.
> - This is how we will be creating layers. Now maybe what we need is to decide protocols for doing things first it will be from how to communicate then what the structure is
> - first step would be defining protocols for running, communicating system, how to add some layer, modify, version it. And the LLM interface that will be doing it. How can things be made proof based can each implementation be first written down proof for.
> - Another problem is storing, naming functions, layers how to find what exist where and how.

Page 2:

> So what I have currently is a box which can support atomic operations, do some actions and output the effect of those ops to the broader world, you can version your stuff. The starting layer access I have to now is what an ACT is. so now we have a system that gives us input output in form of ACT and that is where we have to meet the user. The question for me now is what can I do and how can I do
>
> I should try speaking out and scoping from start yes its all one but we are also building software that is modular.
>
> So given we have the store next up is writing into and to it and having an agent interface how?. Why because it will be the resident builder.

### The question that opened the many

After the first reply, typed by Sid (‹87ffd01d›):

> i woudl first like to understand if the analogy of "box" holds and then what is the abstraction layer then we are building on like previously we had the rama and we did built it out .. we should have a seperation of abstractions because i treated everything as one abstraction in terms of softland and rama like i did not disjoin them and therefore any new idea or work just went over to cover all the abstractions ... i made the distinction of waist, that is an abstraction but now i have stronger pull towards that we should be thinking of the system buildin in abstraction layers so it gets easier to think on top???

### What round 1 proposed (the working is in 01)

- The store's first inhabitant is the builder itself, an agent. This answers
  the 14 July question: "what even is the first thing that will live in
  softland?"
- It met two passages of the vision, each with a way through:
  - "Usable without AI": build the protocol, and make the agent one client of
    it.
  - "A third surface": the builder's why must land in the store. Sid's ask
    becomes an act, and each act of the builder points at it with
    `because-of`.
- The builder's first light: arrive, find, make, run, check, show, adopt,
  return.
  - New code: a host process and the executor; search by words later.
  - Everything else: agreed keys and records.
- Computation above the waist is records (settled 6 September, amended 16
  October).
  - Dependency: a record names what it uses; every run writes down the exact
    versions it used; anything adopted into the base pins its versions.
- Naming: a thing has its own number, with its name written on it. This is
  first-maker decision 3's default.
- Proof first, graded by kind:
  - a protocol gets an executable model;
  - a record gets claims with checks;
  - a capability gets a receipt;
  - usefulness has no proof; only Sid can judge it.

  The precedent is the 24 September formal model.
- Order proposed: one executable model of the first-light protocols, then the
  host, then the executor, then the converters as the builder's first job.

### Questions put to Sid that are still open

1. "Code is going to be stored" (page 1) has three readings: Softland's code
   as material to look at, computation as records, or program code that the
   store runs. The third would reopen the 6 September ruling. The default the
   session told him: the first two.
2. Does the builder's why live in the store from day one? That means Sid's ask
   is an act, and every act of the builder points at it. Default told: yes,
   in this minimal form.
3. Words: "level" for an abstraction, "layer" for the store's layer. Sid
   answered with the fractal layer (below), not with a choice.
4. Nobody answered the order of work that round 1 proposed. Sid moved up to the
   question of levels.
5. The 02b branch ends with three things "only yours, to carry into the
   convergence", word for word:
   - "Is "layer" one word for both the store's space and your fractal unit? I
     now think it can be. And what is the word for a level?"
   - "Should interpretation depth still stop at one executor (6 September), or
     does the VM frame pull you toward a language per level?"
   - "Which promise words should a typist see?"

## The stack (derived in rounds 2, 3 and 5)

Each level is defined by the question it lets the levels above stop asking.
The levels were derived from the vision and the settled decisions, not from
the code.

```
 11  Work          what are we doing, why, what is settled?
 10  Components ┐  what is it made of; how do I make a variant?
  9  Surfaces   ┘  how do I see it and act on it?        (one level on Poly's grounds: 06)
  8  Change        what changed, and which state counts?
  7  Meaning       what is this, what does it mean?      (key meaning may sit lower)
  6  Computation   how is one thing made from another?
 ═════════════════════ the waist runs through 6: the executor is code, records are data
  5  Standing      who did this, and were they allowed?
  4  Layers        which world am I in?
  3  Live          when it changes, how do I learn?      (may fold into 2)
  2  Facts         what is true, now and then?
  1  Log           did it happen, and in what order?
 ─────────────────────
     Rama · JVM · disks

 threads through every level:  who · when · why · about whom
```

- **The store as built.** It is levels 1 to 5 fused (the session's reading of
  the code). Its interface, the act and the read, is what everything above
  builds on.
- **The material axis.** It runs beside the stack, in Sid's 30 July words:
  "three strata — material, Softland code, and host floor". The two axes
  cross; neither replaces the other.
- **Sid's "layer".** It is the unit of composition that repeats at every level
  above the waist; "level" names only the step of abstraction.
  - Round 3 found four relations folded into it: kind of, built on, made of,
    words for.
  - Rounds 5 and 06 found three algebras behind those relations.

Sid's long prompt ‹bb0ad8a0› (the record cannot tell typed from pasted):

> i think i use the word layer as a abstraction itself like a layer can have layers in itself for e.g editor is a layer then code editor and markdown editor and text editors are also a layer in itself and then each layer then has their own layer e.g equivalent of vim, vscode, emacs ... and then a user can combine differents parts from different ones and then its their editor layer which can then be further taken by other users and built on top ... yeah and this is one example an editor in itself is a layer ... fractal to the bottom to the top ... i think like this and thus maybe my vocab also makes is harder for my self and you ....
>
> yeah its giving the users the spectrum and control over the system some knobs do require to be available on different layers/levels imo

## The map

### Agreements across breakouts (signals, not decisions)

1. **The act and the read are the boundary everything meets.** It is the
   instruction set in A, the narrow waist in D, and the shared floor of the
   fractal in E.
2. **Below that boundary sit a few fixed, slow levels, designed with proofs.**
   Above it everything is open, fractal and made of records (A, B, E, G).
3. **The floor is common, small, checked hardest, and versioned by editions**
   (A, D, E, G, J, K, L).
4. **Rama's words stay under the act.** What matters above it is stated as a
   promise or a knob in the upper level's words (A, C, D, F).
5. **The fractal layer is three algebras.** They are delegation (a monoid),
   containment (an operad, with sharing through the Poly diagonal) and
   translation (a functor) (E, H, J, 06). Four coherence laws hold them
   together (06):
   - override commutes with zoom;
   - translation preserves nesting;
   - views commute with override;
   - late binding goes through the final world.
6. **Identity across change is one problem that shows up in many places:**
   diffs, override paths, migrations, revocation, attribution and first-maker
   decision 3 (B, E, J, K, M, 06). If parts have their own numbers, diffs are
   exact; otherwise they are guesses.
7. **Integration and resolution are separate.** Keep everything from every
   parent, and choose among them by a policy on top (C, H, J).
8. **Change is one algebra.** Spans for diffs, pushouts for merges, double
   pushouts for applying (06).
9. **Chains collapse.** Migrations compose into one query, as a tower of
   interpreters collapses into one program (E, J, 06).
10. **Provenance carries much of the load** (B, H, I, K, M).
11. **Every boundary between paces needs to absorb, protect and see** (G, I,
    L, M). Proof effort follows pace (G, L).
12. **Records carry no authority of their own.** A run uses the asker's
    permission, and that is also what gives late binding (K, 06).

### Tensions

| # | Tension | From | Status |
|---|---|---|---|
| 1 | How fat is the act: which slots does the store check, and which does it only carry? | D, round 2 | open |
| 2 | Which parts of the store are truth, and which are views? | C | open |
| 3 | One mechanism or three for the fractal layer? | E | sharpened: three algebras with four laws (05, 06); still open whether Softland exposes them as one mechanism |
| 4 | Does Softland need an operating-system level (processes, a file system, a shell)? | A | open |
| 5 | Where does meaning sit: key meaning low and the catalog higher, or one level? | B | open |
| 6 | Knobs: who may turn which, and at what grain? | F | open |
| 7 | Is pace a property of a layer, or of each builds-on edge? | G | open |
| 8 | Union grain: per place or per part? | H | reframed as Nix's extend-or-replace, a knob (06) |
| 9 | Containment: a tree, or sharing (transclusion)? | E | resolved in principle: identity by reference, display by the Poly diagonal (06); open: whether an override inside a shared part applies per whole or globally |
| 10 | Surfaces and Components: one level or two? | B | one, on Poly's grounds (06); a reading, not settled |
| 11 | Grants: bound to a person, or bearer? | K | open |
| 12 | Declared vocabularies, or open keys? | J | open |
| 13 | Gift, commons or market, and which is the default? | M | open |
| 14 | Should the layer system mirror the social graph, or break it? | I | open |
| 15 | Who is the Linus of a shared layer? | I, M | open |
| 16 | A translation that merges places needs a named policy: which one? | 06 | open |

### Branches not opened

- **Linguistics:** pidgins and creoles, how two languages merge. This is the
  social side of J's pushouts.
- **Cognitive load:** chunking (Miller) and cognitive load theory (Sweller).
  How many words a level can have before a person stops holding it.
- **Law:** Lessig's "code is law". Layers as jurisdictions, and licences as
  checks the gate runs.
- **CRDTs:** merging without coordination. Their algebra, join-semilattices,
  is a fourth candidate structure for combining layers.
- **Ecology:** niche construction, where organisms reshape the environment
  that then selects them. Compare Sid's 23 July line: "the meta layer which
  informs the instances then instances use inform the meta layer further".
- **Christopher Alexander's later work:** *A Pattern Language* and *The Nature
  of Order*.

## Time-sensitive

Until the first real record is kept, the bottom levels can be redrawn for free.
After it, every slot of the act's form stays in the record forever (round 2).
As far as the pages said on 15 October, no real record had been kept. The
window bears on two items:

- The depot inside every act-id (round 2; breakouts A and G). 02b corrects
  round 2: `:streaming` and `:microbatch` are Sid's own decided words for the
  two kinds of depot ("my streaming and microbatch", ‹12c9bd22›,
  `src-store/README.md:117-118`), and they happen to match Rama's topology
  names. What stays open is whether the two words "name a promise (how soon a
  layer's acts are decided) or a mechanism (which Rama topology decides
  them)" (02b). What still holds: whatever sits in an act-id is copied into
  every `replaces`, `based-on` and `because-of`.
- Which slots of the act the store checks, and which it only carries (D).

## What was checked, and what was not

- **Checked against code or pages in this session** (file references are in
  the rounds):
  - The act's form and parts: the act-id `[layer depot uuid]`, fact ids,
    `based-on` ("A label: no gate reads it"), `because-of`, `replaces` and
    `:subjects` (`src-store/softland/store/act.clj`, `src-store/README.md`).
  - The permission id `[who layer granted-in parent allows]`.
  - The read path's `:points-at`, `:from-layer` and `:with`
    (`src-store/softland/store/read_path/query.clj`).
  - The builds-on list as a fact on a layer, nearest wins.
  - "One lock seals one value".
  - "Nothing ends a permission". That is a session's reading, not run.
  - The render rule and the tools rulings (`docs/decisions.md`).
  - The ladder of 3 October (`src/proposal/backend-on-rama-2026-10-02/PROGRESS.md`).
  - The 24 September model (`src/proposal/formal-model-2026-09-24/`).
  - First-maker decision 3 (`src/proposal/first-maker-2026-10-14/`).
- **From the session's knowledge, not checked here:** every outside reference,
  including:
  - Parnas, Tanenbaum, Brand, Kleppmann;
  - Plan 9 and Linux;
  - Spivak and the category theory;
  - the capability systems;
  - the biology and the economics.
- **Derived, the session's own reasoning:** the stack itself, the readings of
  Sid's words, and how the laws apply to Softland.

## Words

- **level:** an abstraction level (this folder's word).
- **layer:** the store's layer, and Sid's fractal layer.
- **Collisions found:**
  - "base" lives on three levels: a layer others build on (4), the system's
    layer that everyone reads (5), and what is accepted (11).
  - "system" spans levels 4, 5 and 6.

  The session's hypothesis, not verified: these collisions are why the
  sign-in work stopped on 15 October with Sid's line "the thing to start from
  is answering and understaindg what i mean by base, system etc."

## Whose words are whose

- **Typed by Sid:** ‹8f0e5667› ("this is what i have been thinking", with the
  two notebook photos), ‹87ffd01d› (the box and levels question), ‹f4596ab1›
  (where we started, the prompt of the 04 branch) and ‹9ec182a6› (stop fanning
  out; converge in a new session).
- **His long prompt ‹bb0ad8a0›** (the record cannot tell typed from pasted):
  few-many-few, the fractal layer, and the knobs line. It holds one paste of
  the session's own text ("Some leaks are honest ..."), which is not his.
- **Session text that Sid pasted, not his typing** (source not traced), quoted
  in the rounds' prompts as it arrived:
  - the 07:02 prompt of the 02b branch (‹11bdc387›);
  - the 07:26 rewrite of ‹bb0ad8a0›, which round 3 answered;
  - the 11:02 message that asked for round 5;
  - the "three bridges" paste that asked for round 6.
- **The two "few" parts** are defined in his go message for this handoff. That
  message was not in the record when this page was written: "for the other 2
  "few" parts, i just mean seeing where we started from now that we have the
  "many" parts, and then the final narrowing down / convergence stretch."
