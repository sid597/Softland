# Round 2: The Box Analogy & Abstraction Levels

## Prompt



<pasted_content id="978c">
 i woudl first like to understand if the analogy of "box" holds and then what is the abstraction layer then we are building on like previously we had the rama and we did built it out .. we should have a seperation of abstractions because i treated everything as one abstraction in terms of softland and rama like i did not disjoin them and therefore any new idea or work just went over to cover all the abstractions ... i made the distinction of waist, that is an abstraction but now i have stronger pull towards that we should be thinking of the system buildin in abstraction layers so it gets easier to think on top???

  wdyt?? don't just agree because of what i am saying ultrathink you don't need any tool except maybe lookin at code docs (and that too iffffff neeeded) to be used for this imo and stay with me on the layer I am talking now don't get demoted learn from your mistakes and pitfls ... think hard deep meta like a true collaborator softwared design for flexibility and information engineer ...
</pasted_content id="978c">


---

## Reply (Verbatim)

I'll stay at this level. Last turn I dropped into what to build first; nothing below does that.

## Short answers

- **Does the box hold?** As a way to look at one level from above, yes. For the store as it is built, not yet. It is two levels in one box, and a Rama word shows on its outside.
- **What are we building on?** A shared store of facts. Every write is an act by someone, in a layer, under a permission. Every read goes through a list of layers, nearest first. Rama was the floor for building that. From here up, nobody should need a Rama word.
- **Should the system be built in abstraction levels?** Yes. But the cause of the spreading goes deeper than Rama against Softland, and I would not draw every level up front.

One note on words. Below, **level** means an abstraction layer. **Layer** keeps its store meaning: a space you write into, which builds on other layers. In this project "layer" already means three things, and that is part of the problem (section 2).

## 1. The box: what holds and what does not

**What holds.** Seen from above, a level should be a box. You know what goes in, what comes out, and what it promises. You never need the inside. Your page 2 says exactly this: atomic operations go in, effects come out, versions are kept.

**What does not hold, in three ways.**

1. **It is not one box.** The store as built is two levels fused together (my reading of the code):
   - *facts*: acts, facts, their order, and reads once or live;
   - *layers and permissions*: who may see or act where, and which layers a read goes through.

   These two change for different reasons. The October decisions that did not hold are all in the second one. Sign-in was built and then taken off main on 15 October, together with "the store's four changes" (commit `3a41c91d`). A change about who is who needed four changes inside the box. Your line that evening was: "the thing to start from is answering and understaindg what i mean by base, system etc."
2. **Its wall leaks.** Every act-id is `[layer depot uuid]`, and the depot is `:streaming` or `:microbatch`, Rama's two kinds of topology (checked, `act.clj`). `replaces`, `based-on` and `because-of` are all built from act-ids, so a Rama routing word sits inside every reference to an act or a fact. Until 9 October the word was "class", a Softland word. Your render rule in `decisions.md` already forbids this: "A layer's vocabulary never appears in another layer's rows." That rule was written for rendering, and I see no sign it was carried over to the store.
3. **It must open.** Carry-on §5 says: "Opening the definition behind an instance should be deterministic and reveal its accumulated record." It also says: "The code editor is a possible deeper zoom into Softland." A sealed box contradicts that. Abstraction means you don't *have to* look inside; it must never mean you *can't*. Clean levels help here, because each level becomes a place to stop when you zoom into how a thing is made.

## 2. Why every new idea spread

You say the cause is that Rama and Softland were never kept apart. That is true. I think it is one case of three deeper causes.

**a. The architecture was drawn as a pipeline.** On 3 October you pointed at the ladder as the architecture map. Its rows look like levels, but each row is a step a thing passes through, top to bottom: data sources, inlets, adapters, converters, act, depots. Its bottom row is a Rama mechanism. A pipeline has no levels. Whatever travels with the data passes through every row: who, identity, time, permission. So every decision about one of those touches every row.

Parnas showed this in 1972 with a small indexing program. If you split a program into modules by processing step, every change spreads. If each module hides one decision that is likely to change, changes stay local. A pipeline is good for tracing one act. It is bad for deciding where a concept lives.

**b. One word, three meanings.** "Layer" is used for:
- the store's space, which builds on other spaces (the closest standard idea is an overlay);
- records composed on records: "I think, basically, the layers are infinite" (6 September);
- today, a level of abstraction.

When one word spans levels, a thought about one level slides into the others.

**c. The waist was asked to do two jobs.** Your definition is: "i use waist as to refer to something that has to exist as code vs what can and should be build on top". That answers one question: what is it made of, and who can change it? A level answers a different question: what must I think about to use it? The two answers mostly line up, but not always:
- A permission grant is low-level, but it is data.
- The grammar reader is code, but it serves a high-level need: formats.
- The meaning of a key sounds high-level, but it must last as long as the facts that use it.

On 2 September you wrote: "we leave other heigher level layers for ecs and call that above the waist". In rendering the two questions lined up, so it worked there. After that, the waist stood in for levels everywhere, and everything below it became one region.

**A qualification.** Someone does have to think in Rama terms: whoever builds the fact level. Keeping them apart means only that person does, and states the result in Softland words. The trouble came when *you* had to decide Softland things in Rama terms: "If I have 10 million users, will I have 10 million partitions?" The Softland question underneath was this: what must a layer promise about the order and speed of its acts? Whoever builds the level answers how.

`★ Insight ─────────────────────────────────────`
- `act.clj` already makes a level argument. The act's shape is compiled into the code, not stored as a fact, because "it must hold when each fact is wrong". A level's interface cannot live inside the material it guards.
- The render rule has a second half: "The engine translates at the entry point." For the store, the entry point is the door. The door could add the Rama routing word on the way in and keep it there. Then the word would never reach an id that other facts copy.
`─────────────────────────────────────────────────`

## 3. What a level is, and how to test one

A level has four parts:
- **Words:** the few nouns and verbs you think in at that level.
- **Promises:** what it guarantees to the level above.
- **A secret:** what it hides, so it can change without anyone above noticing.
- **One direction:** it uses only the words and promises of the levels below it.

Four tests that a boundary between levels is real:
1. **The reasons to change differ.** New storage technology changes the fact level. A new social model changes the permission level.
2. **The swap test.** Could you replace what is below without anyone above noticing? This is a thought experiment to find leaks, not a plan to leave Rama. It has been done once already: the 24 September model ran the store's rules with "No Rama, no network, no disk."
3. **The think test.** Can you work at the level using only its own words?
4. **Pace.** Does the level change at its own speed?

## 4. The levels as I would draw them now

This is a proposal: firm at the bottom, open at the top.

```
 level                   words you think in                          hides
 ──────────────────────  ──────────────────────────────────────────  ───────────────────────────
 open, found by use      meaning · tools · work                      —
                         (kinds, names · converters, views ·
                          asks, claims, candidates)
 computation  (next)     record · step · capability · run ·          which runner: CPU, GPU,
                         budget · what the run read                  or inside the store
 ══════════════════════  ═══════ the box's interface: act + read ══  ═══════════════════════════
 layers & permissions    layer · builds on · who · see / act         how layers and checks
                                                                     are kept
 facts                   act · fact · replaces · read · live         Rama: depots, partitions,
                                                                     topologies, PStates
 ──────────────────────  ──────────────────────────────────────────  ───────────────────────────
 Rama · JVM · disks

 through every level:  who · when · why
```

- **The box is the bottom two levels.** Its interface, the act and the read, is what we build on.
- **Who, when and why run through every level.** This is your 13 September picture: "data transfer is like pipes, elcetricty cables". No level owns them, and every level carries them.
- **"yes its all one but we are also building software that is modular"** (your page 2). Both halves are true. One store holds the material of every level, so there is one address space. The levels are levels of *reading*, not of storage: each level's engine reads the same store in its own words.
- **The waist runs through computation.** The executor is code and records are data. The material of the lower levels (facts, permission grants) is data too. So the waist is a line among engines; it does not split the levels.
- **One caution about the open part.** The meaning of a key may need to sit low and change slowly. Facts must stay readable after the code that wrote them is gone.

## 5. Where I disagree

**Do not design every level up front.** Stewart Brand studied how buildings change over the years. The site, structure, skin, services, room plan and furniture each change at their own speed. A building learns when a fast part can change without disturbing a slow one. His line is "Fast learns, slow remembers." The rule that follows: never cast a fast thing into a slow one, the way wiring gets cast into concrete.
- The **bottom levels** hold data forever. Design them now, carefully, with proofs.
- The **top levels** are records, cheap to change. Let use show where the seams are.

Your 16 October line already holds both halves: abstractions "will come over time as we work more and they will trickle down from top layer to bottom ones and they will bottom out at the code layer but we are tryign our best to have that upfront". Up front is right for the bottom, and trickle-down is right for the top. If we draw seven levels by guess, new ideas will cut across them, which is today's problem in a new form. The OSI network model drew seven layers. In practice two of them, session and presentation, folded into the applications.

**The act is the slowest thing in the system.** After the first real record, every slot in the act's form stays in the record forever. Sign-in was built and taken off main in one day; that is the speed of the levels above the box. So ask one question of each slot: does the store check it, or only carry it? What the store checks belongs in the form. What it only carries could be content instead: facts inside the act, owned by the level that needs them. The internet works this way: TCP's ports and sequence numbers ride inside IP's payload, not in IP's header. One slot this test would question is `based-on`; its own docstring says "A label: no gate reads it." I am not saying it is wrong, only that the test raises the question.

**Hide Rama's words, not its strengths.** An interface that any database could fill would throw away the reasons Rama was chosen: computation next to the data, live reads, and a store that comes out the same when it is run again. Keep those, and state them as promises in Softland words.

**Some leaks are honest.** A person who is typing cares whether an act is decided at once or in a batch. Do not hide that difference. State it as a promise in the upper level's words, such as "decided at once" or "decided in a batch". "Class" was a word of that kind, and "depot" made it Rama's.

`★ Insight ─────────────────────────────────────`
- `replaces`, `based-on` and `because-of` are all built from act-ids. Whatever an act-id holds is copied into the data's relationships, not only into where the data is kept. That is why identity is the slowest part of any store.
- Datomic, Rich Hickey's fact store, keeps its schema as ordinary facts. So the meaning of a key lasts exactly as long as the data that uses it. That is the information-engineering reason to keep "meaning" low.
`─────────────────────────────────────────────────`

## 6. Why now

Before the first real record is kept, the bottom levels can be redrawn for free. After it, they cannot. As far as the pages say, no real record is kept yet; the first-maker runs used a store that was thrown away. So this is the cheapest moment there will ever be to draw the bottom boundaries.

## 7. What changes when you think on top

Every question gets its level first. Your page 1 sorts like this:

| Your problem | Level |
|---|---|
| Where and how to store computation functions | computation; each one lives in a layer |
| If code is data, the problem of dependency | computation: how one record names another |
| Store on the server first, "because that is the interface" | the box's interface |
| Protocols for communicating | facts: the act and the read |
| Add a layer, modify it, version it | layers & permissions |
| Protocols for running | computation's interface |
| Naming functions, finding what exists | above computation, still open |
| The LLM interface | not a level: a user of the interfaces |
| Proof based | not a level: a proof is about one level's promises |

The last two rows matter most. An agent is not a level; it is an inhabitant that uses the interfaces. And "proof based" and "levels" turn out to be the same move. A level's promises are exactly what a proof is about, and that proof may assume only the promises of the levels below. Large verified systems work this way. The proof of the seL4 kernel goes down step by step, and each step is proven to do what the step above it says.

## 8. The next thinking step

This all stays in the chat. First, write the interface of the two bottom levels in Softland words only: their words, promises and secrets. Then list every place where today's store shows something from another level. Each item on that list is one of three things: a leak to move, a promise to state, or something that runs through every level. That list answers whether the box holds.

I'll keep "level" for abstraction and "layer" for the store's layer, unless you want other names; the names are yours.
