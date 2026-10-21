# Round 1: Notebook Pages Reading

## Prompt

[Image #1] [Image #2]  this is what i have been thinking

---

## Reply (Verbatim)

I read your two pages as one line of thought. Page 2 is where you stand: the store is built, and the act is the lowest thing you can touch. Page 1 lists the problems that show up once you try to build on top of it.

Below I check your picture against the code, then take a position on each problem. Everything is a position, not a decision. Nothing is written to disk.

## What I hear

You have a box. Acts go in. Each act is all or nothing. Facts come out, once or kept live. Each layer keeps every change, in order. The act is where everything above has to meet the box, including the user.

Next you want something that lives in the box and builds there: an agent, the resident builder. It writes into the store from outside the browser. It needs agreed ways to talk to the store, run things, and add, change and version layers. It needs a way to find what exists. Each thing it builds should first have a written proof. And one problem is open: if computation is stored as data, how does one piece depend on another?

## What is built and what is not (checked in the code)

| | State | Where I checked |
|---|---|---|
| An act is one or more facts (thing, key, value) from one maker. The gate accepts all of it or refuses all of it. | Built | `src-store/softland/store/act.clj`, namespace docstring |
| An act names who, layer, permission, session, `based-on` (which facts it read), and `because-of` (which act caused it) | Built (the form) | `act.clj`, `shapes` |
| A fact can replace an earlier fact. A fact with no value is a retract. | Built | `act.clj`, `:fact` |
| A layer lists the layers it builds on. A read goes through that list, and the nearest layer wins. | Decided and built | `src-store/README.md`, 11–12 Oct, entry 2; `read_path/query.clj`, `:from-layer` |
| "A maker is a converter, the screen, an agent": one act form for all makers | Built | `door.clj` docstring |
| A read that finds "everything whose key K has value X" | Built | `read_path/query.clj`, `:points-at` |
| An executor that runs records | Not in the store | examples exist in the client, Inland and the rig |
| Sign-in | Taken off main on 15 Oct | commit `3a41c91d` |
| Anything an agent can call | Not built | the door and the read path are JVM code that need a cluster handle |

## 1. The main move: the agent as the first builder

**My position: yes, this is the right next step.** Three reasons:

- **It answers your July question.** On 14 July you asked: "having the substrate what even is the first thing that will live in softland? its like the abiogenisis of softland". You pictured a blank screen then. My reading of your notes is a different answer: the first inhabitant is the builder itself. It needs no screen, because it lives in acts and reads.
- **It tests whether the land explains itself.** An agent arrives with no memory every session. If it can find what exists and act correctly, the land is understandable. Carry-on §5 asks that routine changes be "simple and cheap enough for a local 32B model". The resident builder is the first real test of that.
- **It answers your question of 16 October:** "how can i believe that what you say now is not going to be false in a new session". Today each session keeps what it knows in its own context, and the next one works it out again. If the builder keeps its claims and reasons in the store, the next session reads them instead. Carry-on §4 says "The return is the trust moment." That holds for the agent too.

Your plan runs into two passages of the vision. Each has a way through:

- **"Usable without AI."** Carry-on §1: "AI is a symbiotic aid in this vision, and the platform should remain usable without it." If only an LLM can write into the store, that breaks. The way through is to build no special "agent interface". Build the calls, and make the agent one client of them; a script, a command line, and later the browser use the same calls. The store already works this way, since one act form serves every maker.
- **A third surface.** On 21 July you said that a separate room "is just creating a third surface". If the builder works from a terminal, what it makes lands in the store, but the why stays in the terminal's transcript. The act form already has a place for the why. The smallest step: your ask goes into the store as an act, and every act of the builder points at it through `because-of`.

## 2. Protocols first: the builder's first light

You wrote: first how to communicate, then the structure. I agree. Alan Kay said the same of Smalltalk: in a system that grows, how the parts talk matters much more than what is inside them.

Your list is long, though: running, communicating, layers, versions, the LLM interface, proofs, names, finding. Carry-on §10 warns that "opening five unrelated fronts is evidence to revisit the derivation". To keep it one front, take one walk and define only what it needs. This is the agent's version of your July walk (click, type, reply, leave, come back):

```
 1 ARRIVE   who am I here, and where may I act?
 2 FIND     what exists? what did the last session leave?
 3 MAKE     write a record (a function) into my own layer
 4 RUN      run it on real input; see what it read and what it gave
 5 CHECK    run its claims; each one holds or breaks
 6 SHOW     tell Sid what I made, and why
 7 ADOPT    with Sid's yes, it goes from my layer into the base
 8 RETURN   a new session with no memory continues from step 2
```

| Step | What it needs | New code? |
|---|---|---|
| Arrive | A permission for the agent that ties back to you (decided 8 Oct) | Small, held by the host |
| Find | A start-here entry, a few agreed keys, lookup by name and kind | Mostly no: `:points-at` does name and kind (my reading, not run). Word search is new. |
| Make | An act that writes a record | No: the door |
| Run | Record + inputs + budget → output, plus every read it made | **Yes: the executor** |
| Check | The claim is data, the check is a record, the result is an act | No: run + act |
| Show | Reads that carry writer, time, why | Mostly no: rows carry writer and time; the why is `because-of` |
| Adopt | One act in the base | No engine. Open in the store's queue: how a person's tool becomes the system's, and who may act as the system |
| Under all of it | One process that holds a door and a read path and offers the calls as tools | **Yes: the host** |

So the code to build is the host and the executor, and word search later. Everything else is agreed keys and records. Those are structure, and the builder can write them itself. That matches your 16 October view: "we need engines as code and then other things are examples on top of them".

The "LLM interface" is then just these calls, offered as tools with clear descriptions. You already use one tool made this way: memtool, which works both as a command line and as MCP tools. A good measure of the interface is the smallest model that can make a routine change through it, using only what the land tells it.

`★ Insight ─────────────────────────────────────`
- The store already has places for the why (`session`, `because-of`) and for what an act read (`based-on`). The builder's trail needs no new mechanism, only a habit of filling them.
- `:points-at` is a reverse lookup, from a value back to the things that hold it. That lets a name stay an ordinary fact rather than the thing's identity, which section 4 depends on.
`─────────────────────────────────────────────────`

## 3. Where computation lives, and dependency (problems 1 and 2)

**Where: this is settled** (decisions.md, "Tools are records over a vocabulary"). Computation above the waist is a record: named steps over capabilities, formulas in the leaves, run by one executor. Records live in layers: the system's in the base, yours in yours. Your 2 October line fits this: "make these the canonical ways of uh, providing computation on this on the Softland but as data … you can write your library and other people can import it".

**One thing to check with you.** You wrote "Code is going to be stored." The same ruling says "Nothing in a record is a program." I see three readings:
- (a) Softland's own code, read into the store as material to look at and work on.
- (b) Computation stored as records.
- (c) Program code that the store runs.

The first two fit the ruling. The third reopens it, and the ruling says that question comes to you only when "a record provably cannot say a needed tool twice over".

**Dependency, with a small example.** The system's markdown converter is a record. It uses the markdown grammar, which is also a record. You write a better grammar in your layer. Three questions follow: does the converter use your grammar when you run it? Do facts the old grammar already made change? When does everyone get yours?

| One record points at another… | Like | Gain | Cost |
|---|---|---|---|
| by exact version | Unison, Nix | never breaks; same input, same output | improvements wait until someone moves the pointer |
| by name, looked up at run time through your layers | Smalltalk, Lisp | improvements arrive at once | B can break A silently; the same record acts differently for different people |
| by name, with the exact versions written down on every run | a lockfile | names for people, exact versions for the record | each output must carry what it resolved |

**My position: the third way, with pinned versions for anything adopted into the base.** The store already leans this way. Fact ids never change, `replaces` chains versions, `based-on` records exactly what an act read, and decisions.md says "every result keeps the subject it was built from". Applied to the example:
1. When you run the converter, it uses your grammar, because your layer is nearest.
2. Facts the old grammar made do not change. Each one names the grammar version that made it.
3. Everyone gets your grammar only when it is adopted into the base, as one act, with its checks passed.

**My derivation, not tested:** a layer's builds-on list works like an import path. If so, "add a layer, change it, version it" and "how A depends on B" are one mechanism:
- To fork, make a new layer on the base.
- To change, write in that layer.
- To compare, read through two lists.
- To adopt, make one act in the base.

Carry-on §2 separates automatic history from a meaningful version like a git commit. The store already keeps the first. Adoption is the natural place for the second.

## 4. Naming and finding (problem 7)

This is already an open question: first-maker decision 3, "a function is its name, or has its own number". Its example: on 8 October, 13 files moved into folders. If a function is its name, that move deletes and remakes about 1,500 things, and every pointer to them breaks. The page's default is "its own number" with the name written on it. I agree, because a dependency has to survive a rename. Unison made the same choice: names are labels on things that do not change.

Finding then needs three things:
- **A start-here entry in the base** that says what kinds of thing exist and where to begin.
- **A few agreed keys on everything:** name, kind, what it is for, what it takes, what it gives, who made it, and from what. Takes and gives let you search by shape, for example "which records take a markdown file and give acts?"
- **Lookups:** by name and kind, already possible; by words, new code.

## 5. Proof first

**My position: yes, and this project has already done it once, and it worked.** On 24 September, before any Rama code, a session wrote a small executable model of the store's rules (`src/proposal/formal-model-2026-09-24/`). It generated random histories and checked the eight properties you named after each one. The first round found six places where the rulings said nothing or worked against a property. You settled each one before the build began.

| Thing | Its proof | Example |
|---|---|---|
| A protocol (layers, versions, order) | Executable model, properties over random histories | "An act in my layer never changes a read that doesn't go through my layer" |
| A record | Claims stored beside it as data, each with a check | grammar: "parts add back to the file byte for byte, for every tracked `.md`" |
| A capability | Already decided: "added once, with receipts" | Lean, which you named on 2 October, fits the few pieces that rarely change: the act form, the gate, the record language |
| Usefulness | No program can show it; only you judge it | "Can I make the next thing with it?" |

Two limits, both from the vision:
- **Proof belongs at adoption, not on every sketch.** Carry-on §8: "Working hypotheses and raw thoughts need room before reaching that status." The working layer can hold unproven sketches, and the base takes only proven ones.
- **A proof does not show usefulness.** Carry-on §10: "passing technical checks, or receiving several agents' approval did not answer those use questions."

If claims are facts in the store, a session that disagrees has to show a result that breaks the claim; a different opinion is not enough. That is your 16 October conjecture format, kept in the land instead of in one chat.

There is a larger shape here, which is my reading. Building Softland this way is carry-on §8's research loop at small size, with Softland itself as the first subject:
- The working layer holds the candidates.
- The claims and checks are the reasoning map.
- The base holds what has been accepted.

`★ Insight ─────────────────────────────────────`
- The 24 September model was property-based testing: generate many histories, then check rules that must hold after each one. Its value was finding gaps in the rulings while changing them still cost nothing.
- Records are total and run under a work budget, so every run ends. That makes "run the check" a real proof step, since a check always gives an answer.
`─────────────────────────────────────────────────`

## 6. What I would do first

Define, then build. This order is a proposal for you to change:

1. **One small executable model** of the first-light protocols: communicate (act in, facts out, an answer for every act), layer life (make, shadow, compare, adopt), and pinned against late-bound dependency. Its properties are the proofs you asked for. With no Rama, it runs in seconds.
2. **The host:** one process offering `act`, `read` and `find` as tools. With only this, an agent can already make layers, write facts, read them back and find things by name.
3. **The executor.** The 16 October session wanted it first, "because every other data piece is written in it". I put it third, because without the host nobody can call it.
4. **The builder's first job:** the markdown and Clojure converters, written as records, run and checked. Softland's code then enters the store, built by its first inhabitant.

Records that wake by themselves when a fact arrives come later. In first light, the builder runs things by hand.

## Two questions

1. **"Code is going to be stored": which reading?** Softland's code as material to look at, computation as records, or program code the store runs? If you say nothing, I take the first two, and the 6 September ruling stands.
2. **Does the builder's why live in the store from day one?** Say the builder writes a grammar. Should the store hold only the grammar, or also your ask that started it? My default: your ask goes in as one act, and every act of the builder points at it. The full conversation comes in later.
