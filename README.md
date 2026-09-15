# Softland

> this is what i want to build with softland ... RAMA + Hyperfiddle Electric + webgpu +LLMS + humans
>
> so this is not going to be a any single person maintaining anything ... we should imagine a vibrant ecosystem of humans and agents on different layers of it. Its fully scalable in rama, electric is the reactive layer, webgpu is the view layer because eventually we will be able to make 3d interactive models for say drug discovery or understanding structures in the same platform and like run local llms etc. then the llm layers as i described more below, softland is not a standalone person thing its a social network for the collective intelligence for all of us .. its like the idea map for the builders what can be now beuild on top of science and when they get to building the paradigm is same in the same softland ecosystem just a different layer
>
> The problem I want to solve is, how to answer the question "What are the known knowns, known unknowns, and unknown unknowns about xyz?"
>
> Why? Because I think this is a fundamental question which will give me and others interested, a reasonable attack on understanding how to achieve the outcome of humans living healthy forever.
>
> As of now it seems to me we can speculate a few potential solutions but we can't even evaluate these solutions. The map that we have as of now is fragmented, hard to make sense and keep track of new progress. What is my proposal for this new map?
>
> When viewed on a flat screen it is like google earth, you can zoom in and out of the knowledge map, each nation is similar to topics like biology, physics etc. you can see how these different topics are connected then you start zooming in and you see more detailed data. You can zoom in all the way at which point you see different node types, like question, claim, evidence, source. Different zoom levels represent different levels of understanding and exploration, based on how zoomed in you are, there is AI that has the context of zoom level. It shows you topics you can explore along with some summaries, videos etc. of the underlying data, interacting with the knowledge at this level. The knowledge map AI has a default way of explaining things but the user comes along with their own AI assistant tuned to how the person wants to understand and interact with the map. These assistants can do different types of actions based on who they are assistants to. This would not be an AI exclusive or dependent platform but it is more of symbiotic nature, AI is there to get the boring tasks out of the way and let humans with different levels of understanding explore, understand, and contribute to their level of knowledge.

*From [vision/LOG.md](vision/LOG.md). That file is my words only, verbatim, append-only. Everything else in this repo is a reading of it.*

## Where this started

Discourse graphs: https://network-goods.notion.site/The-Discourse-Graph-starter-pack-312374c813b24ec6b4d53a054371ee5a

Ontological logs: https://arxiv.org/pdf/1102.1889.pdf

Collective intelligence:https://topos.site/work/collective-intelligence/

Applied category theory:https://arxiv.org/pdf/1803.05316.pdf  



---

We have Google earth to explore any part of the world, but we don't have anything like that for knowledge. 

What is the current state of progress on autoimmune diseases, what and how well do the experts understand it? who is working on it? 
how far have they got? what are all the explored directions? how can I understand, contribute, use and spread it? 

We need a place to get the best view for the current state of things, and contribute our part.

How can I start from 60k feet view and get to 30k? Maybe read a book, listen to some lectures, podcast etc. I have tried
they don't work, it feels like its working but for me, I have realised over time its more of insight porn (for me) than anything else.

There is no REPL, no visual model I can break, play, or engage with. For 60k feet view of biology in 2024 I would expect a 3d interactive
model of Caenorhabditis elegans (C. elegans). 

Time travel should be possible in 2024, atleast for recorded knowledge. I should be able to ask how we got here?
We need a massive interactive "God created the integers" for all knowledge. I should be able to start 2-3000 years back
and rediscover for myself. 

A scientific paper should not only be a paper with text written on it and the reader develops the model in their brain. 
We should be able to upload a paper and see what is the diff from the current model, we should be able to replicate it 
via a simulation. Maybe as I go through section by section and the 3d model is updating itself. 

The paper should not be a paper in current format imo, it should be a log, sometimes I am more interested in knowing how
than what, I want to feel what the author felt, I want to do the work. So when I am reading the log I am building the world
piece by piece, maybe I want to stop at some point and then think by myself, I want to re discover it and not know it. 

Science should be distributed, open and accessible for all the humans. University is not. 
State of what we call "collaboration" in physical world is not what I mean by collaboration. The "collaborative" tools
in the software space just mean here are cursors of all the people. CRDT is the cutting edge topic in collaborative software
which I think is the first layer of abstraction over "here are cursors of all the people". Conflicts in a collaborative 
software are considered to be bad, yes they are by current definition at the current level of abstraction. If the abstraction
layer is higher conflicts are good for progress. 

My very strong intuition as of april 14 is that category-theory is THE missing piece I have been looking since the starting
of this project as a mere thought. I think so because would provide me with the tool that lets me think about the structure and coherence.

---

Random musings on how things should be: 

If llms or any ai for that matter has to become a true collaborator to a human then it should be highly malleable, can't 
expect the same linear chat ui to be used for creation, brainstorming, synthesising, do some task, collaborate.
Paper is used differently while in different phases of work. 

Maybe the essence of the system should be how you want to feel while interacting with it and long after the interaction is done.
Maybe its more about the feelings that the action, how to emulate "pure feelings" in these systems, I mean we should be able to
because we can with "art" which in itself is a communication system. 

The system should be viewed as an art one can engage with, I think one strong litmus test would be "have you used it in your dreams?"

## How it is built

**Rama** is the truth. Everything that happens goes in as an event and the interface reads materialized state out. There is no state anywhere else. The server ingests files, git history and transcripts and cuts them into addressable units, keeps material in revisions, and projects pages from it.

**Electric** is the reactive layer between the store and the screen. In Inland a gesture becomes an Electric recipe, then a proposal, then a Rama admission, then an Electric recomputation, and the screen follows from that.

**WebGPU** is the view layer. One engine draws text, images, paths and bounded 3D scenes through one executor, with one set of coordinates, colour, GPU transport and render targets under all four. Below the waist is compiled code, and only what is physical belongs there: fill, paint, composite, camera, pointer, clock.

> land it · A: data · B: GPU

Everything above the waist is data:

> we have build engine and some primitives that get hardcoded and we leave other heigher level layers for ecs and call that above the waist. Anything above the waist is stored as data, is collaborative editable, createable, agents and humans can build freely over it without getting into git merge deadlocks, anything in wcs layer should be creatable and then saved for reuse or build higher order things from it

> The problem is not we need to make some set of things in code to use later … the. Problem is what needs to be coded with the assumption that we are saying anything that is not here can be done on ecs layer and the layer can go to i finte compositions and drawings idk imaging anything that is the pinnacle for anything that 2d is possible

The engine is built full breadth, never as an MVP:

> I think this should not and never be an mvp-shaped instict this should be full breadth build and from that lens we build the decision WE NEED FULL ENGINE

The range it has to hold in one frame: tldraw-like drawing, Figma-like design and composition, Blender-like 3D scene composition, and text editing. One address space, many projections over it:

> ONE SUBSTRATE TO RULE THEM ALL

**AI** works inside the same store. Transcripts are material like everything else. A resident inside Inland answers a real request and the reply is kept as a record. Models are meant to be readers, makers, semantic markers and context gatherers, so that

> I don't have to be hunter and gatherer of context only verifier

**Humans** point, write, draw, and decide. The current build is Inland, under `src-inland/`: authored instruments on Electric and Rama, drawn with the engine above. You point at a thing, open the definition behind it, change it, keep it, and the next click uses it.

## Where it goes

The five unlocks, in order of how much power they give:

> 1. Design Unlock: Everything buildabel and controlable in frame using agent and mouse, keyb
> 2. Editor and mouse pan zoom control each object in frame. Have a way to write to a container
> 3. Semantic breaking of block types
> 4. Representation of semantic blocks
> 5. 3D Render
>
> 2. is where I say we would have the power the full brunt of softland because once we have this every other thing I can just directly build into it.

> What we want is a tool to build the tool in some design, deploy and use it all at once all from softland.

The first subject is Softland's own codebase: understand it from inside, then change it from there.

> maybe the best thing we can do is have like all the affordances to be able to run all of these in context nd we don't have to pre bake anything ui is used according to the nature of chat

The conceptual model being thought through right now is in [src/proposal/frame-2026-09-15/](src/proposal/frame-2026-09-15/): everything is a fact about a thing with provenance, consumers are found by matching, one gate writes, and a person's walk through a question is traced fact by fact.

Then the same machine on science:

> we first break down the paper into its reasoning units e.g questions, claims, evidence, artifacts etc. this build a map of reasoning which will be sitting in an existing similar map of the field then say a human or agent starts looking into it and now has to do their sensemaking and building on top they will have their working layer to take notes, ingest data, write down hypothesis raw thoughts, think about experiments, theories etc. these can be many many ... then there is a visual representation in 2d (one like the map of questions, claims etc .. discourse graphs (https://discoursegraphs.com/)  or ologs (David spivak) canvas/visualisation algos etc.) and 3d rendering of the physical layer at hand of that field all 3 synced on the data that is being thought about and hence if someone adds a new reasoning to the dg (discourse graph) then it would mean the 3d changed in some way, how (the agent will tell what has changed) and ask to draw only that part keeping others constand […] imagine this being done by 10s of agents per person and 100's of people deploying their agents on this problem .... the base layer (in this example) is something that would be accepted by the either proof/experiments/reasoning that can be rederived and verified

> I think maybe intuitively i am building the architecture of softland to how it should be ideally like maybe my intellectual goal is like protocol or architecture book equivalent idk … i like rama for their implementation, product but also the principles they build on

## Run it

```sh
bin/inland up
```

Then open http://localhost:8127. Needs Java and Clojure, Node, a Rama distribution, and a WebGPU browser. `bin/inland status` and `bin/inland down` do what they say. What to try once it is up is in [docs/builds/inland/README.md](docs/builds/inland/README.md).

## Reading the repo

- [vision/LOG.md](vision/LOG.md) is the source. My words, never edited.
- [docs/carry-on.md](docs/carry-on.md) is a thematic reading of the log. Start there if the log is too much.
- [docs/decisions.md](docs/decisions.md) is what is settled. [docs/how-we-got-here.md](docs/how-we-got-here.md) is why the build changed.
- [src/app/](src/app/) is the canonical client and server: [client map](src/app/client/README.md) for rendering, [server map](src/app/server/README.md) for Rama, ingest and material.
- [src-inland/](src-inland/README.md) is the current build.
- [src/proposal/](src/proposal/) holds fact bases and proposals for the next session.
- [history/](history/README.md) keeps earlier attempts with their reasoning. [reference/](reference/README.md) keeps dependency docs and source snapshots.
- [CLAUDE.md](CLAUDE.md) is where an agent starts. `AGENTS.md` points to it.

Research code. It breaks.
