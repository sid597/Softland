The proposal is about one decision.

You are building a place that people and agents change from the inside, including the parts they use to change it. In such a place every piece is one of two kinds. Fixed: it can only change by stopping the place and rebuilding it from outside. Live: it can change while the place runs, by someone in it.

The problem is where that line goes. Which pieces must be fixed. And what the fixed pieces have to guarantee so that everything live stays coherent no matter who changes it, how often, or with what tools.

Coherent has to mean at least this:

- When something you built on changes, you find out. Always. And you can see how sure you ever were.
- Every change says who made it, when, and what they were looking at.
- Many people and agents work at once, each in their own working copy, without blocking each other.
- Things from outside enter, code, a model's answer, the clock, another copy of the place, and none of the above breaks.
- The place cannot lie. It never shows a guess as a fact or old as current.
- It stays affordable as the number of people and agents grows.

The second half of the problem is how you would know the line is drawn right before you have built everything on it.

What it asks of you is the kind of response a line gets, not a design. What is on the fixed side that should not be. What is missing from it. And whether the check it proposes is the right check.


> very good excellent work ... now just before we get to layer 0 ... what kind of response does the proposals want from me? the one you proposed here is the one that is most specific.

It wants more than one kind, and they come in an order. The one I gave you is near the end of it.

First, recognition. The proposal is a reading of your own words. Before it asks anything else it needs to know whether it read you right. Where the picture is what you meant, and where it drifted. A "yes, that's it" or "no, I meant" on the whole, not on parts.

Then, adoption. Whether you take this as the way of thinking from here. Not whether it is correct. A frame is chosen, not verified, and everything after is measured against it. A no here makes the rest moot.

Or, instead of adopting, breaking. It invites you to name a case it cannot carry. It says that would be worth more than anything else in it. This is the strongest form of no, and the most useful.

Then, the line. For what it puts on the fixed side: does each belong there, and what is missing. This is the one I gave you.

Then, positions. Where it took a stance and said so: hold it, strike it, or change it. Item by item.

Then, the check. Whether the way it proposes to know the line is right is the way you would know.

Then, leaving open. It names things that get settled later, by the first real use. It wants you not to answer those now.

Then, go. Whether to take the next step it names, and which.

And words. It proposes words. It wants to know which you will use, because those become the words you think in.

The proposal is explicit about positions, breaking, the check, and what stays open. It is silent about recognition and adoption. Those are the two it needs first.

On every one of these it needs the weight. Whether you are ruling or leaning. It will be built on, and a lean read as a ruling anchors the wrong thing.

What it does not want: approval of a design, implementation choices, or answers to what it says is settled by use.


>  layer -1 reads right, and the order you gave is the layer order. recognition first, but i cannot take the whole at once, which is why i am here, so recognition goes layer by layer too. start with the top layer only: what the proposal says i meant, in one paragraph, in my words, marking where it is quoting me and where it went beyond me, so i can say yes that's it or no i meant on that paragraph. Add whatever you think is right for this response there is no word limit as such but scope limit.

<!-- The reply below was inserted by the 16 September session from its context, verbatim. It was missing between Sid's message above and his "what is one substance?" -->

Italics are what the proposal attributes to your own words, in the log or your notes. Where I write "past that," the proposal went on by itself. I have read nothing but the proposal this session, so "yours" means the proposal says so, not that I checked the log.

You asked for the person first, then the world, and you asked that the loop be derived from what must be true rather than from a floor someone had already cut, because you were right to ask *why those steps*. You meant a place where *the map must not lie*, where *an approximate resolution presented as exact is a calibration failure*; past that, the proposal makes not-lying one machine that covers stale, guessed, and drifted alike. You *refused the word type because it is constraining and built upfront* and asked for *the ECS equivalent*; past that, it says a thing is nothing but what has been said about it, shape lives on values and never on things, and the tools are made of the same stuff as what they work on. You asked whether there is such a thing as *normal data*; past that, it answers that there is not, only data with fewer current uses. You said an action has both *the file's view and the person's journey view, with agents waiting on each and maybe a meta one*, and that this was *hard to think upfront*; past that, it says nobody has to think it upfront, because both views fall out of recording every crossing with who did it and on what basis. You *half remembered* that you never routed by metadata, and the record agrees; past that, it makes that a law: nothing is found by name, only by matching, so nobody routes. You described *pointing at the type of an instance and doing it again until the top*; past that, it says levels are computed by following demand upward, and self-reference stays tame only because names resolve at each pass and never mid-pass. From your May design, *the world changes only if accepted*; past that, it keeps that rule whole and drops the split into three families. Via carry-on, you meant that each person works in an active layer against a base, kept inspectable and comparable before it becomes the next base; past that, it splits your one word "layer" into four different axes and says only one of them is something you write down. You wrote that *authentication and authorization should come first because it solidifies*, and that from anywhere the question is *what am I allowed to query, down to the atomic packet and nested through groups*; past that, it says what solidifies is only the crossing where reads leave the store, and who may see what is soft, seeded by a root actor. You said *the depot already versions finely* and the question is *the atomic unit that carries meaning*; past that, it answers that the atomic unit is whatever someone points at, and a thing gets an id when something needs to refer to it. You said one thing *must genuinely remain outside the land forever*; past that, it names it as the runtime and defines it mechanically as the one consumer that is not a fact. And you asked to replace *compiled, computed, tool, CRUD, readback, normal data, layer* and to place *compiler, deploy, readback*; past that, it did, and the words it chose are its own.

Where I would look hardest, because the step past you is largest or changes the question:

- **One substance.** Everything above rests on the proposal's move from "no types" to "everything is a fact, tools included." You asked for no types. You did not, in what it attributes to you, ask for one substance. If you did not mean that, most of the rest changes.
- **Auth.** You said first because it solidifies. The proposal narrows what solidifies to the mechanism and makes who-sees-what soft. If you meant decide who sees what early, that is a divergence, not an extension.
- **The journey.** You said hard to think upfront. The proposal says it is not hard, it is free. If you meant there is a real design question in there, it has dissolved the question rather than answered it.
- **The atomic unit.** You asked what carries meaning. It answered what gets an identity. Those may be the same question to you or two different ones.

 what is one substance?

Everything in the place is made of the same kind of record, and there is only one kind.

You asked for the ECS equivalent. ECS has three kinds of thing: an entity, which is an id; a component, which is data hung on an id; and a system, which is code that runs over components. The proposal keeps the first two and drops the third as a separate kind. A thing is an id. A fact is data hung on an id, with who, when, and on what basis. And a tool is a fact too: its "what it wants and what it gives" and its steps are hung on an id like anything else. So the passage, the view that shows the passage, the rule that fires when you point at it, the shape a value must have, the policy saying who may write where, and the record of what the gate decided are all the same kind of record in the same store.

"No types" alone does not give you that. You could have no types and still keep content in a store, tools in code, and permissions in a config file, three different substances. The proposal's step is that there are not three. There is one, and the runtime that reads it is the only thing made of something else.

That step is what makes "change the place from inside with the same moves you use on what is in it" possible. Point at a view the way you point at a passage. Relate a rule to a clause. Save a tool the way you save a note. It is also what makes the runtime the one exception: it has to be a different substance because it is what interprets the substance.

> ok I agree can you present the pair in a table format so i can more easily grok

<!-- Sid's message above was inserted by the 16 September session from its context, verbatim. The reply below was already in the file. -->

Left column is what the proposal attributes to you. Middle is where it went past that. Right names the kind of step, and marks the four where I would look hardest.

┌───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┬───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┬───────────────────────────────────┐
│                                                                   You said                                                                    │                                                      The proposal made of it                                                      │           Kind of step            │
├───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
│ The person first, then the world. Why those steps?                                                                                            │ The loop is derived from what must be true for a person to change something and have the world respond, not from a floor someone  │ extended, method                  │
│                                                                                                                                               │ already cut.                                                                                                                      │                                   │
├───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
│ The map must not lie. An approximate resolution presented as exact is a calibration failure.                                                  │ One machine covers stale, guessed, and drifted alike, and paints all three.                                                       │ extended                          │
├───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
│ Refused the word type, constraining and built upfront. Asked for the ECS equivalent.                                                          │ A thing is only what has been said about it. Shape lives on values, never on things. Tools are made of the same stuff as content. │ extended, largest: one substance  │
├───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
│ Is there such a thing as normal data?                                                                                                         │ No. Only data with fewer current uses.                                                                                            │ answered                          │
├───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
│ An action has the file's view and the person's journey view, with agents waiting on each and maybe a meta one. Hard to think upfront.         │ Nobody thinks it upfront. Both views fall out of recording every crossing with who did it and on what basis.                      │ dissolved the difficulty          │
├───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
│ Half remembered never routing by metadata. The record agrees.                                                                                 │ Law: nothing is found by name, only by matching. Nobody routes.                                                                   │ hardened, memory to law           │
├───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
│ Pointing at the type of an instance and doing it again until the top.                                                                         │ Levels are computed by following demand upward. Self-reference stays tame because names resolve at each pass, never mid-pass.     │ extended, mechanism               │
├───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
│ May design: the world changes only if accepted.                                                                                               │ Rule kept whole. The split into three families dropped.                                                                           │ kept                              │
├───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
│ Via carry-on: each person works in an active layer against a base, kept inspectable and comparable before it becomes the next base.           │ Your one word "layer" split into four axes. Only one of them is something you write down.                                         │ split                             │
├───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
│ Authentication and authorization should come first because it solidifies. What am I allowed to query, down to the atomic packet and nested    │ What solidifies is only the crossing where reads leave the store. Who may see what is soft, seeded by a root actor.               │ narrowed, possible divergence     │
│ through groups.                                                                                                                               │                                                                                                                                   │                                   │
├───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
│ The depot already versions finely. The question is the atomic unit that carries meaning.                                                      │ The atomic unit is whatever someone points at. A thing gets an id when something needs to refer to it.                            │ answered, maybe a different       │
│                                                                                                                                               │                                                                                                                                   │ question                          │
├───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
│ One thing must genuinely remain outside the land forever.                                                                                     │ The runtime. The one consumer that is not a fact.                                                                                 │ named                             │
├───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────┤
│ Replace compiled, computed, tool, CRUD, readback, normal data, layer. Place compiler, deploy, readback.                                       │ Done. The words it chose are its own.                                                                                             │ replaced                          │
└───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┴───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┴───────────────────────────────────┘


> yes all pairs i agree


Top layer recognized. Recognition now goes one layer down, part by part. Here are the parts, by what each proposes, one line each, nothing below that.

1. What everything is made of. One kind of record, hung on ids, carrying who, when, and on what basis. No types.
2. What does things, and how it is found. Tools are records too. Each says what it wants and what it gives. Found by matching, never by name.
3. How change moves. A tool gives back either a running answer that recomputes on its own, or an offer to one gate. Which one is not the tool's choice. The gate checks and either writes or says why not. Nothing else writes, from inside or from outside.
4. How it stays honest. Every read leaves a trace with how sure it was. Stale, guessed, and drifted are all found from the traces. Time is stamped, never read.
5. How many people share it. Each in their own layer over a base. Who sees what is enforced at the one place reads leave. Branches are layers. Conflicts are recorded, not blocked.
6. What stays outside, and how outside gets in. The runtime is the one thing not a record. Commits, model answers, clocks enter as offers with provenance made for them at the door. A rebuild is an ask to the host, and the answer comes back the same way.
7. What is fixed, and how to check the cut. Nine things fixed for life. A gauge. Nine demonstrations. What stays open and what settles each.
8. The current code against the frame. What exists as a sibling of what, and what the first workpiece has to show.

My trace and its findings sit under parts 1, 3, and 4 and are positions, not readings of you. They come up when those parts do.

For each part, recognition is the same pair form as before: what you said, what the proposal made of it, kind of step. I would take them in this order. Say the word and I open part one, or name a different one.



> ok yes I follow seems right to me

<!-- Sid's message above was inserted by the 16 September session from its context, verbatim. The reply below was already in the file. -->

Part one, recognition. What everything is made of.


> What is included in "everything" and what is not included in "everything" do we have a answer at this level? what is a value? 
>
> my readings:
> "A thing" is just an id, anything that is said about this id/thing is what makes it a component. To know what is the component-type of this thing, get all the things that have been said about it. Its dynamic.
>
> "A value" is a ______ it has a "shape" which is a "grammar", the grammar is itself versioned. So to talk about a "shape" of a "value" we need to get its grammar-at-version. 
>
> what is a component vs a value from english word pov both seem kinda similar, how does thing, component, value combine??
>
>
> can we diagram it at this level??
>
> "A record" is the collection of things, versioned and authored by. 
>
> " The question is the atomic unit that carries meaning" 
>
> atomic unit of what? and why is question the only one? what does "question" mean? 
>
>
> "The basis has three kinds of entry, not one" 
>
> what is "basis"??
>
> I think this reply of mine also communicates that there is still  a layer missing between the eight lines and this table

<!-- Sid's message above was inserted by the 16 September session from its context, verbatim. The reply below was already in the file. -->

You are right. Between the eight lines and the table there is a layer I skipped: the words of part one, introduced in order. The table used "record," "value," "basis" before any of them was said. Here is that layer. I will use the proposal's word "fact" from here on; "record" was my plainer word for the same thing, and it misled you: a record is one fact, not a collection.

The words of part one, in the order they depend on each other.

- Thing. An id. Nothing else. A function, a passage, a person, a tool, a rule, a layer, a shape: each is a thing, and being a thing means only that it can be referred to.
- Fact. One statement about one thing: "this thing, in this respect, is this." The whole place is a pile of facts and nothing else.
- Attribute. The "in this respect" of a fact. Which kind of statement it is. Its source. Its text. Its members. Its definition. The attribute is the name of the slot.
- Value. The "is this" of a fact. The content. The actual text of the passage. The actual set of member ids. The actual definition of a tool. A value can be a string, a number, an id, a set of ids, a structure with named parts.
- Component and value are not two things. In ECS a component has a name and data. Here the attribute is the name and the value is the data. Thing plus attribute plus value is one fact, and that is the ECS component hung on the ECS entity.
- Shape. What form a value takes. Text, a number, a set of ids, a structure with these named parts.
- Grammar. The written-down description of a shape. A grammar is itself a fact, on a thing that stands for that shape, and it has versions like any fact. To know what shape a value must have, you look up the grammar its attribute uses, at the version that grammar is at now.
- Provenance. Beyond thing, attribute, and value, every fact carries who said it, when, and in which layer, meaning whose working copy.
- Basis. What the sayer was looking at when they said it. The list of facts they read, each with the version they saw and whether the read was exact. "I related f to c" has the basis "f at version 2, c at version 4." The basis is what lets the place say later: "f has changed since you said that."
- Request. The fact whose arrival caused this one to be made. The chain of why.

How they combine, drawn.

                       ┌─ attribute ─┐   ┌─── value ───┐
   thing f1 ──has────  │  :source    │   │ "(defn f …)" │      ← the statement. ECS: entity + component
                       └─────────────┘   └──────┬───────┘
                                                │ has a shape: "text"
                                                └── described by a GRAMMAR, itself a fact on
                                                    another thing, at a version

   the same fact also carries:
       who ─── alice          when ─── t          layer ─── A            ← provenance
       basis ── [f1 at v2 (exact), c1 at v4 (exact)]                     ← what she was looking at
       request ─ the pointing that led to it                             ← why it was made


   thing f1 ──has── :source ─── "(defn f …)"
            ──has── :continuity ─── {name "f", hash h1}
            ──has── :relates-to ─── c1               ← this value is an id, so it is another thing

   the type of f1  =  {:source, :continuity, :relates-to}    computed from its facts right now. Never stored.

What "everything" includes, and what it does not. Yes, we have the answer at this level. Included, as facts: the content, meaning passages, functions, clauses; the tools, meaning views and rules; the grammars; the policies about who may write where; the gate's own decisions; who is watching what; where someone pointed. Not included: the runtime that reads the facts and runs the tools; signals that never landed as a fact, like a mouse moving; the rendered screen itself; the actual file on disk, of which the place holds only the fact made from it; a model's insides; the outside world itself, of which the place holds only facts about arrivals from it.

"The question is the atomic unit that carries meaning." You parsed "the question" as a noun being defined. It is not, and it is not a Question node either. The sentence attributed to you reads: the depot already versions finely, and the question that remains is: what is the atomic unit that carries meaning? Atomic unit of material. The smallest piece of text or code that a statement can hang on and still mean something. The depot versions whole blobs. Whether a relation should hang on a file, a function, a paragraph, or a range of characters was the open question. The proposal's answer is that it is not decided upfront: whatever gets pointed at gets an id, so the unit is settled by use.

With those words in place, the table from before can be reread. Every term in it is now introduced.



> - Thing. An id. Nothing else. A function, a passage, a person, a tool, a rule, a layer, a shape: each is a thing, and being a thing means only that it can be referred to.
>
>
> does this not mean much much computation even for a simple thing?? and if its a nested structure then we have to recomputed the dependency tree to refer to everything iff we wanna update? i think i am missing some conceptual thing

<!-- Sid's message above was inserted by the 16 September session from its context, verbatim. The reply below was already in the file. -->

The thing you are missing is that nobody ever gathers "everything about a thing." Three moves in the frame make the cost you are imagining not exist, and one cost is real and should be named.

Reference, not containment. When a value is an id, it points. It does not include. If a question's members fact says {p1 f1 c1}, reading it gives you three ids and nothing behind them. You follow an id only when you need what is behind it. So when f1's source changes, the question's members fact is untouched. It still says {p1 f1 c1}, and that is still true. Nothing about the question changed, so nothing about it is recomputed. Nesting inside a value works the same way: a document can sit as one fact with a big nested value until someone needs to point at a paragraph inside it, and only then does that paragraph get an id. You do not decompose things into ids upfront. That would be the waste you are picturing.

You read the one fact you need, not the thing. A tool that wants a function's source reads exactly one fact: f1, source, latest. One lookup. "Its type is the set of attributes it currently has" is what type would mean if someone asked in general, for instance a halo asking what can be done with this. It is not a step on every access. This is how Datomic works: an entity is just an id, everything about it is datoms, and nobody computes the entity, they read the datoms they need.

Change moves along recorded reads, not along structure. When a new fact lands, the place does not walk any tree. It asks one question: who read the fact this replaces? That answer is a lookup, because every read left a basis entry and the basis entries reversed are an index from fact to readers. Only those readers are marked stale. Whether they re-run is a policy: a view someone is watching re-runs now, an unwatched one stays marked until asked. Each re-run reads only what it reads, so a cascade goes exactly as far as things actually changed and no further. A spreadsheet is the same shape: change A1, and only cells whose formulas read A1 recompute, then theirs. Here the basis is the recorded formula inputs.

   what you imagined                         what the frame does

   update f1                                 append one fact: f1 source v3
     └─ recompute everything that              └─ lookup: who read f1 source v2?
        contains or points to f1                  └─ mark those stale
          └─ and everything that                     └─ re-run the ones being watched
             contains those …                           └─ each re-reads only its own reads

The real cost, named. The frame pays on read so that change is a lookup. Every read leaves a basis entry, and that is storage and write volume on every pass, not just on every change. That is the trade the frame makes, and it is what buys staleness, the journey, and "what was I looking at" for free. The other real cost is the one the proposal already flags: when a fact arrives, finding which rules care about it means reading each candidate's conditions, and that grows with how many rules and agents are in the place. Neither is a tree walk. Both are proportional to what actually reads and what actually changes.



> ok so I think the framing of "you said" is not helping as it should like we imagined it would evoke something or anchor .. nopes so lets drop that from now on .. I am anchored to the layer -1 then the recognition and the eight lines you wrote. I am also not sure if we should be carrying ecs and how things are defined in it, I used it to name a concept and i think now its pretty baked in our layers above so we can keep things simple as in not naming an abstraction and one implementation in it. cool so lets redo the layer down from these words


Part one, redone. What everything is made of.

What it proposes. Words in the order they depend on each other.

A thing is an id and nothing else. Being a thing means only that it can be referred to. A function, a passage, a person, a view, a rule, a working copy, a shape: all things.

A fact is one statement about one thing: this thing, in this respect, is this. The place holds facts and nothing else.

The "in this respect" is the fact's attribute: which kind of statement it is. Source. Text. Members. Definition.

The "is this" is the fact's value: the content. The text itself. The set of ids. The definition itself. A value can be a string, a number, an id, a set of ids, or a structure with named parts. An id inside a value points; it does not include. You follow it only when you need what is behind it.

A value has a shape: the form it takes. The written description of a shape is a grammar. A grammar is itself a fact, on a thing that stands for that shape, versioned like any fact. To know what shape a value must have, look up the grammar its attribute uses, at that grammar's current version. The fixed part reads grammars. The people and agents inside write them.

A thing has no type. Ask what a thing is and the answer is the set of attributes it currently has facts for, computed when asked, never stored.

Every fact also carries who said it, when, and in which layer, meaning whose working copy it lives in.

Every fact carries its basis: what the sayer was looking at. The facts they read, each with the version they saw and whether the read was exact. This is what lets the place say, later, that something has changed since.

Every fact carries its request: which fact's arrival caused this one to be made. Empty when nothing inside caused it, which means it came from outside or from a person acting directly.

Grain is not decided upfront. A thing gets an id the moment something needs to refer to it. Whatever brings files in mints ids for named forms, because reference by name is what it serves. A person mints one by pointing at a range. Minting is a fact with an author. "This version is that thing" is a fact too.

Everything, then, means: the content, the views and rules, the grammars, the policies about who may write where, the record of what was let in and what was refused, who is watching what, where someone pointed. Not included: the one thing outside that reads the facts and runs the tools; signals that never landed as a fact; the rendered screen; the file on disk, of which only the fact made from it is held; a model's insides; the outside itself.

                        ┌─ attribute ─┐    ┌─── value ────┐
   thing f1 ──has────   │  :source    │    │ "(defn f …)"  │        the statement
                        └─────────────┘    └──────┬────────┘
                                                  │ has a shape: text
                                                  └── described by a grammar: itself a fact, versioned

   the same fact also carries
       who ── alice        when ── t        layer ── her working copy
       basis ── [f1 at v2, exact · c1 at v4, exact]        what she was looking at
       request ── the pointing that led to it              why it was made

   thing f1 ──has── :source ──── "(defn f …)"
            ──has── :continuity ─ {name "f", hash h1}
            ──has── :relates-to ─ c1                    an id: points to another thing, includes nothing

   what f1 is  =  {:source, :continuity, :relates-to}    computed now, from its facts. Never stored.

What it settles. Fixed for the life of the place, from this part.

- A thing is an id. Nothing about it is built in. No "is a."
- Values have shapes. Grammars are facts with versions. The fixed part interprets them.
- What a fact carries: thing, attribute, value, who, when, layer, basis, request. The fixed part chooses this envelope, and it is the one shape nobody inside can change.

What it leaves open. With the position my trace took where it took one.

- What a basis entry looks like when the read was of a pattern, finding many or finding none, or was a stand on something outside the place, like a commit. The proposal gives one entry shape. The trace's position: three.
- What a version is. Per fact, per thing, or per attribute. The trace's position: a running number the place assigns to each fact as it lands, and "newer" is a relation between two facts, not a counter kept on the thing.
- Whether one thing can hold many values under one attribute. The trace's position: no. One current value per thing, attribute, and layer. Anything many-valued, like a relation, is a thing of its own, because something will point at it.
- Whether "was the read exact" needs a third value for identity matched by name rather than by content. The proposal leans yes. The trace's position: no; how the match was made goes in the value, and exact stays yes or no.
- How the envelope itself changes. The proposal: by rebuild with migration, shape settled by the first one.
- Erasure. An append-only pile resists it. The proposal's position: a tombstone above, a physical drop below, tested by the first real second person.

What it asks of you.

- The line. Do the three settled items belong on the fixed side. Is anything missing from what a fact carries, or anything there that should not be.
- Positions. On each open item where the proposal or the trace took a stance: hold, strike, or change.
- Words. Thing, fact, attribute, value, shape, grammar, basis, request. Whether these are the words you will use.



>  is it possible to say like a thing is an ID and uh, that is for life okay that is cool but let's say we have a thing which has attributes and values uh, at that point can we say like define another thing that points to this old thing let's call them thing 0 and thing 1 thing 0 is the old one which has attributes and values thing 1 is the new one and thing 1 has attributes let's say name and uh, canonical or something I mean the basic concept is let's say we have something that gets uh, settled as thing but we can build a next layer on top of it and never use the thing as a raw thing so hence we are basically uh, kind of making type system as emergent because a thing x can refer to a bunch of things which define similar kind of things is that making sense

  I agree with the second settled point. As for the what a fact carries, uh, this seems right as of now, but uh, I cannot say for future or if this will hold for everything. That I am not sure. It might. I don't know.

  uh, what I am not uh, sure about how do we define a group of things as something like how do we make emergence or hierarchical or some kind of uh, from category theory maybe there is a better word for this yeah but uh, I'm not able to see that concept like uh, when I was reading your version point I was thinking of this uh, we can version individual things but if I am on a higher level let's say if I ask a general query what was done on this day what does this query resolve to do we look for everything in the system and only get the parts that were done on that day or is it more like maybe the code changed on this day very much but the notes that I am looking for did not change and for this query I only am interested in the notes but not the code and since everything is versioned how do we draw this line and how like from the versioning point of view uh, yeah are you getting it




Yes, I am getting it. Three separate things, and the frame has an answer to each. The answer to the third is the one I think you are missing, and it is the same answer as to the first.

A type is a thing that other things point at. Take thing 0 with its attributes and values. Make thing 1, give it a name and whatever else, and on thing 0 assert one more fact: thing 0, kind, thing 1. That fact is ordinary. It is versioned, authored, in a layer, with a basis. Now thing 1 is a type in the only sense the frame has: things point at it, and rules match on the pointing. A rule that wants "all passages" matches facts whose kind is thing 1. Point at thing 0, follow kind to thing 1, follow thing 1's own kind fact upward, and you have the recursion until the top, with no level built in. What the frame refuses is only a built-in "is a" that the fixed part enforces. What it allows is any "is a" that someone asserts, and that is exactly the emergent type system you are describing. Two people can even disagree about what thing 0 is a kind of, each in their own layer, and both are facts. "Never use the raw thing" is then a discipline your rules impose, not something the floor imposes: write your rules to match on kind, not on attributes, and the raw thing is never touched.

   thing 1  ──has── :name ── "passage"
            ──has── :kind ── thing 9        (a kind of kinds, if you want one; follow it upward)

   thing 0  ──has── :text ── "…"
            ──has── :kind ── thing 1        ← one ordinary fact. This is the whole type system.

   thing 7  ──has── :text ── "…"
            ──has── :kind ── thing 1        ← same kind, so the same rules match it

On the envelope, your hesitation is the right weight. Thing as id, yes. Shapes as grammars, yes. What a fact carries, right for now and unknown for later: that is a lean, and I will not read it as more. It is also the one place in the whole frame where being wrong is expensive, because every other change is a new fact and this one is a rebuild. The proposal's own check for it is whether use keeps demanding a new slot. If the first workpiece needs a ninth slot, the envelope was cut wrong, and we will know early.

A group is a thing, and the version of a group is the basis of whoever read it. This is the piece I think was missing. A group is thing g with a fact: g, members, {p1 f1 c1}. Hierarchy is groups pointing at groups. Nothing built in. Now versions. Each member has its own versions. The membership fact has its own versions, changing only when membership changes. A group does not have a version of its own, and it does not need one, because anything that reads the group leaves a basis: "I read g's members at version 3, then p1 at 18, f1 at 2, c1 at 5." That basis is the version of the composite. Two readings of the group are compared by comparing their bases. "Did this group change since yesterday" means: does yesterday's basis differ from what those same reads return today.

That is also how your query draws its line. "What was done on this day" is a pattern, meaning a description of which facts you want, by any of their slots. Every fact carries every slot, so the pattern can say: when is today. Or: when is today and attribute is text. Or: when is today and thing is a member of my notes group. Or: when is today and layer is mine. The place does not decide whether code counts as notes. You decide, by what you put in the pattern, and there is no special feature for it because the slots are already on every fact. Nor does it look through everything: facts are indexed by their slots, so a pattern by day hits the day index and a pattern by day and attribute hits both.

And for the case you named, where code changed a lot and the notes did not: your notes view reads note facts. Its basis holds only note facts. The code changing touches none of them, so that view is not stale, and "did my notes change" is answered by asking whether that one view is stale. The line between code and notes is drawn by what the view reads, and nothing else has to know it exists.


> very cool and awesome ... yeah the envelope will become solidified as we build more pre-backed substances for a comfortable layer on hiher user end ... for version yes it needs to be there .. i don't have more on this layer tbh


Envelope: agreed, and that is the right test for it. It proves itself by how much can be baked on top without touching it. Version: yes, it stays. Part one closed.

Part two. What does things, and how it is found.

What it proposes. Words in the order they depend on each other. The proposal's word is consumer; I will say tool.

A tool is a fact. Its thing is an id like any other, and its value is two parts: a signature and a body.

The signature says what the tool wants and what it gives. What it wants is a pattern: which kinds of facts, meaning which attributes, and under which conditions. A condition is a check on the current situation, read at the moment of matching: what mode is on, what is selected, which tab is open, whose working copy is active. That situation is the context. What it gives is a kind of fact, plus whether what it gives is a running answer or an offer. Part three says what those two are.

The body is the steps. It is written in a small fixed vocabulary of leaves, plus the names of other tools. The place never reads the body. Only the fixed part runs it. Everything the place knows about a tool is in its signature.

A name inside a body is a reference. It is resolved to the current version of the named tool at each run, never copied in. This is why editing a tool takes effect on the next run and not before, and why the run in progress finishes on the old version. That boundary between runs is what keeps a tool that edits tools from eating itself.

A tool is never called by name. It is found by match. A fact lands. The place looks up which tools want that kind. For each candidate it reads the conditions against the context. Those that pass, run. Nothing decides where a fact goes. The fact lands; the tools that want it find it.

Trigger is why a tool runs now. Someone wants its answer. A fact of a kind it wants landed. Something it read has changed since. A long job it owns is reporting. Something arrived from outside. Match is the mechanism for the first two.

A fact's degree is how many tools currently match it. A paragraph with a painter and a reader has two. Add a model lane and it has three, and the paragraph did not change. "Normal data" is data of low degree, not a different sort of data.

What can be done here is the tools that would match a pointing at this thing, in this context. It is computed, not listed anywhere. A halo is that set painted around the thing.

A pointing alone does not say what the person means: select, annotate, relate. Context does. The rules for those three have conditions that read the context, and the context is facts: what is already selected, what mode is on. The place forbids expressing that choice as a slot in the fixed part, because a slot there is a level nobody inside can edit. When two tools that both give offers match the same trigger in the same context and layer, that is a miss. It is not resolved by priority. It is recorded as a fact with a subject and a time.

Placement is a rule saying which indexes a kind of fact goes into. Placement is itself a tool. So adding a kind of fact is asserting a grammar and a placement rule. Adding a tool is asserting a signature over kinds that exist. Neither needs the other to change, and nothing has to see the future.

Levels are computed. A tool over passages wants passage facts. A tool over that tool's definition wants tool facts. A tool over the running code's forms wants the facts made from those forms when they were brought in. Same shape, different pattern, no mechanism per level. Point at a thing, follow what wants it, and follow again, and you climb until the pattern would have to match the one thing outside.

   a tool, as a fact
     thing t1 ──has── :tool ── { signature: wants  { kinds #{:pointed}, conditions [mode = relate, something selected] }
                                            gives  { kind :relation, offer }
                                 body: steps over leaves + names, resolved at each run }

   match
     a fact lands (:pointed, target f1)
        └─ index by kind: which tools want :pointed?  → {halo, select, relate, inspect}
             └─ read each one's conditions against the context
                  select: nothing selected? no.     relate: selected and mode relate? yes.
                  inspect: mode inspect? no.        halo: no conditions. yes.
                       └─ relate and halo run. Nobody named either.

   levels
     tool over passages ───── wants :text
     tool over that tool ──── wants :tool
     tool over the runtime ── wants :unit, the facts made from its forms
     same shape. Climb until the pattern would have to match the one thing outside.

What it settles. Fixed for the life of the place, from this part.

- A tool is a signature and a body. The signature is a pattern in with context, and a kind out with its nature. Signatures are facts. Tools are found by matching.
- Placement is a rule over kind, and the rule is itself a tool.
- References resolve at each run. Nothing is copied from a definition into a thing.
- What the set excludes: no list of tools, no list of kinds, no routes, no stored levels.

What it leaves open. With the position my trace took where it took one.

- Whether an index is rebuilt when its placement rule changes, or old entries stay. The proposal doubts any index needs to outlive its rule.
- Whether the thing that runs tools can itself be a tool, admitted rather than deployed. Named as research with its own evidence bar, not architecture now.
- The leaf vocabulary. Every leaf is a rebuild, so which leaves exist is the contested seam, and the rate at which use demands new ones is the gauge. Part seven returns to this.
- "What can be done here" as match now, versus match one context fact ahead. The trace's position: one ahead, painted with what each tool still needs, because after selecting with no mode set, match now offers nothing.
- The miss condition. The trace's position: only tools that give offers can collide. Tools that give running answers never compete, because they are not choices.
- Whether a tool is only ever found by match, or also invoked by a reference from another body. The trace's position: both, and both leave a basis entry, because the staleness view had to be invoked from the surface's body or it would trigger on its own output.
- Whether a tool declares that it runs inside or reaches outside, or the fixed part derives that from which leaves its body uses. The trace's position: derived, because a declaration can lie.
- Cost. Candidates by kind, times condition reads per candidate. Named as the number to watch at tens of agents per person. Not open, but not yet measured.

What it asks of you.

- The line. Do the four settled items belong on the fixed side. In particular whether "found by matching, never by name" is fixed, or whether there is a case where naming a tool to run is unavoidable.
- Breaking. This is the part where a counterexample is cheapest to try: name a tool you would want that cannot be written as a pattern plus a body, or a situation where the context cannot carry the intent.
- Positions. On the trace's four stances: one ahead, offers only, match or reference, derived not declared.
- Words. Tool or consumer. Signature, wants, gives, body, reference, match, trigger, degree, halo, miss, placement.


> so a tool means like it needs to run and here we are only defining the shape So how does this uh, become a thing that is runnable? And by runnable, I mean like every time someone depends on it, we are not uh, first getting the data, then making it a build and then running it. Okay, sorry, my mental model might not be correct in this regard. And it could be in the code layer we are already making a renderer or some kind of engine that makes these tools available at the compile time. So what my worry is like a user should not be waiting too much to use a tool uh, because on the engineering side we decided it to be as data for some other reasons. From the user point of view while they are working they should not feel any lag in their workflow and also not consume like exuberant uh, memory.

  > A tool is never called by name. It is found by match. A fact lands. The place looks up which tools want that kind. For each candidate it reads the conditions against the context. Those that pass, run. Nothing decides where a fact goes. The fact lands; the tools that want it find it.

  I both agree and uh, not agree with this. By not agreeing, I mean like uh, I think uh, maybe we did not think through or something. Yeah, I'm not sure. Let's let's just really not agree. Um, the part that I agree with is like having giving a tool a name makes it non-modular in some sense. Like it makes it modular to up to some point. Uh, but over time it just starts accumulating many more things that is not needed by everyone so it turns into in its own abstraction uh, and the thing that I not agree with is like naming is in itself a matching kind of thing yeah so not very sure but like the solution for the condition condition is read against the context and that decides the match that seems right to me yeah

  > What can be done here is the tools that would match a pointing at this thing, in this context. It is computed, not listed anywhere. A halo is that set painted around the thing.

  A pointing alone does not say what the person means: select, annotate, relate. Context does. The rules for those three have conditions that read the context, and the context is facts: what is already selected, what mode is on. The place forbids expressing that choice as a slot in the fixed part, because a slot there is a level nobody inside can edit. When two tools that both give offers match the same trigger in the same context and layer, that is a miss. It is not resolved by priority. It is recorded as a fact with a subject and a time.

  what does "here" refer to in this context? Mm, and yeah, it's like uh, we use one tool to hold something, and then by hold, I don't mean like a point and hold kind of thing, but let's say in we, we are trying to say like. Uh, select this and uh, I will do some more things over this but at this point of time register this action so that I can take further actions and this is like becoming a context hmm hmmmm

  How are we solving this? Are we? Can we?

  um, sorry I am not uh, very sure on what uh, placement means can you see more about it what is indexes I'm confused by placement because I was thinking like everything is an ID so it can be anywhere or something but I think here it means something else the word placement I mean

  everything else in the settled I agree without



Four threads. Taking each.

Runnable, and the lag you are worried about. The sequence you are picturing, get the data, build it, run it, is what happens when a leaf changes. It is never what happens when a tool runs. A tool's body is steps over leaves. The leaves are compiled code inside the fixed part, loaded once when the place starts. Running a tool is walking its steps and calling leaves. That is a compiled engine interpreting a small piece of data, the same way a spreadsheet does not compile a formula, a database does not compile a query, and a rule engine does not compile a rule. Nothing is built at use.

Where the cost of one run actually goes: resolving each reference, which is a lookup by name to the current version, not a fetch; walking the body, which is short because anything heavy is a leaf; and leaving the basis entries, which is the cost already named. Memory: bodies are small data, the engine exists once, and running answers are never stored. The only wait a person should ever feel is a real computation the tool does, such as asking a model, and that is an outside crossing shown as pending, not hidden behind a spinner pretending to be done.

Why tools are data at all, from the user's side and not the engineer's: so that changing a tool is a fact, effective next run, with no build. The build is reserved for leaves. The frame pushes as much as possible above the build line precisely so that people rarely wait on one. If use keeps needing new leaves, that is the gauge saying the vocabulary was cut wrong.

Naming versus matching. Your pushback is right, and it sharpens the rule rather than breaking it. A name is a pattern that matches exactly one thing. The frame has that: a reference inside a body resolves by name to the current version. So the frame does not forbid naming. What it forbids is a name held by the store: a standing rule that says facts of this kind go to that tool. The difference is not the mechanism, which is the same lookup. The difference is who holds the name. A name inside a body is changed by editing that tool, which anyone inside can do. A name in the store's routing is a level nobody inside can edit. Stated properly: match wherever the fixed part would otherwise have to hold a name; name freely inside bodies, because bodies are material. Your accumulation worry lands on the second half. A body that names many tools becomes the thing that accumulates. That is real, and it is a discipline for whoever writes bodies, not something the floor can prevent.

"Here," and holding. Here is the thing under the pointer plus the session's facts at this moment: what is selected, what mode is on, which tab. What can be done here is the tools whose pattern would match a pointing at this thing, given those facts.

What you describe, select this, register that I did, do more over it later, is solved, and by nothing special. Selecting is a rule that fires on a pointing and lands one fact in the session layer: selected, f1. That fact is the registration. Every later rule that cares has a condition that reads it. Each further action lands another session fact. The context is that pile, and it is built by acting. When you are done, a rule tombstones the selection. So a hold is a fact in the session layer that later conditions read, and nothing is held anywhere else.

   point at f1      → select rule fires      → session: selected f1        ← the hold
   choose relate    →                        → session: mode relate
   point at c1      → relate rule fires      → its conditions read: selected? mode relate?  yes
                                             → relation f1 → c1, with basis [selected f1, the two pointings]
   done             → clear rule fires       → session: selected ⊥

Can we, and are we? Yes. This is exactly what the trace walked, and the relation's basis shows what was held when it was made.

Placement and indexes. You are right that a thing is an id and an id lives nowhere. Placement is not about where a thing lives. It is about where a fact gets listed so it can be found without reading everything. An index is a lookup: by kind, to the facts of that kind; by thing, to its facts; by day, to that day's facts. Without indexes, every match and every query is a scan of every fact in the place. A placement rule says: when a fact of this kind lands, list it in these indexes. The fact is not moved. Entries are added. One book, one identity, listed in the author catalog, the subject catalog, and the year catalog. Placement is the rule for which catalogs a new book goes in.

It is a tool rather than part of the floor so that which indexes exist can change from inside. Add a kind of fact, assert a placement rule for it, and nothing below moves. The open question from before is about this: when a placement rule changes, does its index get rebuilt, meaning the index is a running answer over the rule, or do old entries stay.

With that, the settled item reads: which listings a fact goes into is a rule anyone inside can change. Whether it still belongs on the fixed side is yours to say.


> cool so are there any open questions at this point for this

<!-- Sid's message above was inserted by the 16 September session from its context, verbatim. The reply below was already in the file. -->

Yes. Sorted by what each needs.

Yours to say now. Four stances the trace took that the proposal did not. Hold, strike, or change each.

- What can be done here is match one context fact ahead, painted with what each tool still needs, not match now.
- Only tools that give offers can collide into a miss. Tools that give running answers never compete.
- A tool runs either because it matched or because another body referenced it, and both leave a basis entry.
- Whether a tool runs inside or reaches outside is derived by the fixed part from the leaves its body uses, not declared by the tool.

Left to building, not to you now.

- When a placement rule changes, whether its index is rebuilt or old entries stay. The proposal doubts any index needs to outlive its rule. The first rule change will show.
- The cost of matching: candidates by kind, times condition reads per candidate. Named as the number to watch. Not measurable until there are tens of agents in the place.

Belong to a later part.

- Which leaves exist, and how often use demands a new one. Part seven, where the gauge lives.
- How long session facts live and who clears them. The hold you asked about ends when a rule tombstones it, but the policy for a session's lifetime is part five.
- The thing that runs tools being itself a tool, admitted rather than deployed. Research with its own evidence bar. Not architecture now.

Not open, just named. A body that names many tools becomes the thing that accumulates. The floor cannot prevent it. What it can do is make it visible: how many bodies reference a given tool is a pattern over references, so a hub shows up as a view, if someone writes one.


> agree nothing to say more here from my side


Four positions held. Part two closed.

Part three. How change moves.

What it proposes. Words in the order they depend on each other. The proposal's words are conclusion and claim; I will say running answer and offer.

A tool gives back one of two things, and the two never mix.

A running answer is derived and never stored. It passes to the next tool, or to the screen. When anything it read changes, it recomputes. Because it is never stored, a past running answer is not lost: the tool at its version plus the reads at their versions is enough to make it again, so pointing at "what that view showed yesterday" means pointing at that pair.

An offer is a fact that wants to exist. It carries everything a fact carries except the two things only the gate can give, the running number and the time, and it carries one thing a fact does not: the version it expects to be current for that thing, attribute, and layer. That expectation is how it can be refused if someone else got there first.

Which of the two a tool gives is not the tool's choice. A tool may give running answers only if it is replayable: the same reads always produce the same answer. No clock, no randomness, nothing from outside. A person, a model, a build, the world are not replayable, so what they give can only be offers. The fixed part enforces this, reading it off the leaves a body uses, as part two settled.

Offers come from two directions. From inside: a rule firing, an editor saving, a long job reporting. From outside: a commit, a model's reply, a tick of the clock, a sensor, another copy of the place. Both directions arrive at the same door.

The door is the gate, and it is the only thing that writes. It makes three checks. Shape: the value fits the grammar its attribute uses. Version: the offer's expectation matches what is actually current. Policy: this actor may write this kind into this layer. Shape and version are fixed. Policy is facts, changeable from inside. If all three pass, the gate strips the expectation, stamps the number and the time, and the offer is a fact. If any fails, nothing is written.

Either way the gate's answer is itself a fact, a decision: admitted with the number given, or refused with the reason, the policy rule that answered, and, for a version failure, the fact it lost to. A decision's basis is exactly what the gate checked, so "why was this refused" is always traceable.

Nothing else decides where an offer goes. The tool does not know. The gate does not branch on who made it beyond what policy says. The fact lands with its kind, placement lists it, and the tools that want it find it, as part two said.

Change moves in a pass, five stations. Why a tool runs now. What it reads, with the context deciding which layer wins. What it derives. What it gives: an offer to the gate, or a crossing outward to the screen, the host, or a mind. And maintain: any running answer whose reads have moved runs again. How eagerly is a policy of where it sits: at once, while someone is watching, or on a budget. On every arc of the pass ride two things that are not stations: who and on what basis, and a status for every read, which is value, absent, pending, or failed. A read that has not returned yet says pending. It never forces a false answer.

A tool that runs long is owned, has a budget, and has a lifecycle. Its progress and its outcome are offers.

One more thing must be an offer. Inside the place, a running answer reaching another tool is just a read. But a running answer reaching a person's screen or a model's prompt has crossed a boundary, and what was shown, built from which reads, is recorded as an offer. That is what tells the journey what you were looking at, and what gives a model's reply a basis, without storing a single rendered page.

   a tool gives
     running answer ── never stored · passes on · recomputes when its reads move   only if replayable
     offer ────────── to the gate · carries who, basis, request, and an expected version

   inside  ──┐
             ├──▶ GATE ──▶ fact   or   decision: refused, reason, rule, lost-to
   outside ──┘     shape · version · policy
                   fixed   fixed     facts

   a pass
     trigger ─▶ read ─▶ derive ─▶ offer | cross out ─▶ maintain: reads moved? run again, by policy
     ═══ on every arc: who + basis, and a status: value · absent · pending · failed ═══

What it settles. Fixed for the life of the place, from this part.

- What a tool gives is a running answer or an offer. Running answers pass and recompute, and only replayable tools give them. Offers enter one gate, from inside or outside, carrying who and on what basis.
- The gate checks shape, expected version, and policy over actor, layer, and kind. Policy is facts. Only the gate writes.
- Maintain is by the policy of the boundary. Budgets, cycle limits, and failure values are fixed. No leaf may be non-replayable.
- A running answer that crosses to a person or a model is recorded as an offer, with what it was built from.

What it leaves open. With the position my trace took where it took one.

- Whether a long job's progress is offers along the way or only a final outcome. Settled by the first rebuild, not before.
- Whether every recompute that reaches a person is an offer, or only some. This is the volume driver of the whole place, and it is a store question, not a frame question.
- Whether decisions sit in the same numbering as facts, or apart. Store question.
- What the request of a pass is when no single fact started it, for instance a standing watch. The trace's position: the fact whose landing started this pass, with the watch in the basis.
- Whether a decision is frozen, or recomputes when policy changes. The trace's position: frozen, but because it has a basis, "which past refusals would pass now" is a running answer anyone can ask for.
- When something outside lies, a build that claims success and did not succeed, provenance makes the lie attributable, not impossible. The mitigation is that outcomes carry who and basis, so a check can be run again and the disagreement becomes a fact. Named as the one place where the store being truth is a stance rather than a guarantee.

What it asks of you.

- The line. The four settled items. Two in particular: "only replayable tools give running answers," and "only the gate writes."
- Breaking. Name a thing a tool could give that is neither a running answer nor an offer. Or a change that must get in without passing the gate.
- Positions. The trace's two: the request rule, and decisions frozen but queryable.
- Words. Running answer or conclusion. Offer or claim. Gate, decision, expected version, replayable, pass, maintain, status.
