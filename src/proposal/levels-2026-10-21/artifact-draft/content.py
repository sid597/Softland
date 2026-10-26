"""What the Levels page quotes, and from where.

Every `X(src, text)` below is a verbatim excerpt of one source file. build.py
checks each one against its file before it writes the page (runs of whitespace,
such as line wraps, are collapsed on both sides before comparing). Text the
page writes in its own voice (labels, short guides) lives in build.py and
template.html, never here.

Sources: '01' ... '07' are the round files in the folder above, 'MAP' is
MAP.md, 'LOG' is vision/LOG.md.
"""

REGISTRY = []


class Ex:
    """One verbatim excerpt: `md` must occur in source `src`."""

    __slots__ = ("src", "md")

    def __init__(self, src, md):
        self.src, self.md = src, md
        REGISTRY.append(self)


def X(src, md):
    return Ex(src, md)


# --------------------------------------------------------------------------
# Whose words. Kinds follow MAP.md "Whose words are whose".
# --------------------------------------------------------------------------

BADGES = {
    "hand": ("Sid", "handwritten",
             "His notebook page. The text is a model's transcription; the photo is the source."),
    "typed87": ("Sid", "typed ‹87ffd01d›",
                "Typed by Sid, as MAP.md and vision/LOG.md record it."),
    "typed9e": ("Sid", "typed ‹9ec182a6›",
                "Typed by Sid, as MAP.md and vision/LOG.md record it."),
    "long": ("Sid?", "long prompt ‹bb0ad8a0›, typed or pasted",
             "His long prompt. The record cannot tell whether he typed it or pasted it."),
    "go": ("Sid", "go message",
           "From his go message for this handoff. It was not yet in the record when MAP.md was written."),
    "pasted": ("session", "session text Sid pasted",
               "The record shows this as session text he pasted, not his typing. Its source is not traced."),
    "older": ("Sid", "an earlier line, as a round quotes it",
              "His earlier words, as the session quoted them in a round. This page did not check them against the record."),
    "noprov": ("?", "provenance not given",
               "MAP.md does not say who wrote this."),
}

# --------------------------------------------------------------------------
# Overview: the method, in Sid's words, and the two "few" parts.
# --------------------------------------------------------------------------

FEW_MANY_FEW = X("MAP", 'we do start out in a single point but then there are soo many breakouts and then later convergence just like the neural network images few-many-few')
GO_MESSAGE = X("MAP", 'for the other 2 "few" parts, i just mean seeing where we started from now that we have the "many" parts, and then the final narrowing down / convergence stretch.')
TYPED_9E = X("LOG", 'second, this is getting too much for me to read, so once you integrate those three, stop fanning out we need to do the othe two "few" parts but this session is too big to have your best pov on how to do the convergence and how so i think we need a new session')
NOTHING_DECIDED = X("MAP", "**Nothing here is decided.** Every level, agreement and tension is a session's position unless it is marked as Sid's.")

# --------------------------------------------------------------------------
# Where it started.
# --------------------------------------------------------------------------

OPENING_LINE = X("01", "this is what i have been thinking")

# Bands are (top %, bottom %) of each photo; `sorts` are rows of round 2's
# table "What changes when you think on top", which puts each page-1
# problem on a level of round 2's first drawing.
NOTEBOOK_BANDS = {
    1: [(1.5, 4.6), (4.8, 8.6), (8.6, 16.4), (16.4, 30.5), (30.5, 40.0),
        (40.0, 57.5), (58.8, 87.0), (87.0, 96.0)],
    2: [(4.5, 45.2), (45.4, 60.2), (60.2, 75.6)],
}
NOTEBOOK_SORTS = {
    (1, 1): [X("02", "| Where and how to store computation functions | computation; each one lives in a layer |")],
    (1, 2): [X("02", "| If code is data, the problem of dependency | computation: how one record names another |")],
    (1, 3): [X("02", '| Store on the server first, "because that is the interface" | the box\'s interface |')],
    (1, 5): [X("02", "| Protocols for communicating | facts: the act and the read |")],
    (1, 6): [X("02", "| Add a layer, modify it, version it | layers & permissions |"),
             X("02", "| Protocols for running | computation's interface |"),
             X("02", "| The LLM interface | not a level: a user of the interfaces |"),
             X("02", "| Proof based | not a level: a proof is about one level's promises |")],
    (1, 7): [X("02", "| Naming functions, finding what exists | above computation, still open |")],
}
NOTEBOOK_NOTES = {
    (1, 3): X("MAP", '"Code is going to be stored" (page 1) has three readings'),
    (2, 0): X("04", "**Seed 1, the ground: the box.**"),
    (2, 1): X("04", "The whole inquiry is about how both halves of that sentence can be true at once."),
    (2, 2): X("04", "**Seed 2, the aim: the resident builder.**"),
}

FIRST_LIGHT = [
    # (step, question from the walk, "what it needs" row of round 1's table)
    ("ARRIVE", X("01", "who am I here, and where may I act?"),
     X("01", "| Arrive | A permission for the agent that ties back to you (decided 8 Oct) | Small, held by the host |")),
    ("FIND", X("01", "what exists? what did the last session leave?"),
     X("01", "| Find | A start-here entry, a few agreed keys, lookup by name and kind | Mostly no: `:points-at` does name and kind (my reading, not run). Word search is new. |")),
    ("MAKE", X("01", "write a record (a function) into my own layer"),
     X("01", "| Make | An act that writes a record | No: the door |")),
    ("RUN", X("01", "run it on real input; see what it read and what it gave"),
     X("01", "| Run | Record + inputs + budget → output, plus every read it made | **Yes: the executor** |")),
    ("CHECK", X("01", "run its claims; each one holds or breaks"),
     X("01", "| Check | The claim is data, the check is a record, the result is an act | No: run + act |")),
    ("SHOW", X("01", "tell Sid what I made, and why"),
     X("01", "| Show | Reads that carry writer, time, why | Mostly no: rows carry writer and time; the why is `because-of` |")),
    ("ADOPT", X("01", "with Sid's yes, it goes from my layer into the base"),
     X("01", "| Adopt | One act in the base | No engine. Open in the store's queue: how a person's tool becomes the system's, and who may act as the system |")),
    ("RETURN", X("01", "a new session with no memory continues from step 2"), None),
]
FIRST_LIGHT_UNDER = X("01", "| Under all of it | One process that holds a door and a read path and offers the calls as tools | **Yes: the host** |")
FIRST_LIGHT_SO = X("01", "So the code to build is the host and the executor, and word search later. Everything else is agreed keys and records.")

SEEDS = [
    {
        "id": "s1",
        "short": "the box",
        "title": X("04", "**Seed 1, the ground: the box.**"),
        "quote": X("04", "a box which can support atomic operations, do some actions and output the effect of those ops to the broader world, you can version your stuff. The starting layer access I have to now is what an ACT is."),
        "badge": "hand",
        "packed": X("04", "Packed inside: the box is one thing; it is closed; Rama is part of it; the act is a good face for it."),
        "to": ["B", "C", "A", "D"],
    },
    {
        "id": "s2",
        "short": "the resident builder",
        "title": X("04", "**Seed 2, the aim: the resident builder.**"),
        "quote": X("04", "next up is writing into and to it and having an agent interface how?. Why because it will be the resident builder."),
        "badge": "hand",
        "packed": X("04", 'Packed inside: an agent needs "an interface"; functions, dependency and names are one problem; every implementation can have a proof; the builder needs control; other people will build on what it makes.'),
        "to": ["A", "K", "B", "E", "F", "H", "I"],
    },
    {
        "id": "s3",
        "short": "levels",
        "title": X("04", "**Seed 3, the method: levels.**"),
        "quote": X("04", "i treated everything as one abstraction in terms of softland and rama ... i made the distinction of waist, that is an abstraction but now i have stronger pull towards ... abstraction layers so it gets easier to think on top"),
        "badge": "typed87",
        "packed": X("04", 'Packed inside: "layer" means one thing; the waist is the main boundary; the levels can all be drawn up front.'),
        "to": ["E", "D", "A", "B", "G", "J"],
    },
]
SEED_ROOT = X("04", '**"The question for me now is what can I do and how can I do."**')
SEED_TOGETHER = X("04", "Taken together, the three seeds ask three things: what the floor is, who builds on it, and how to think about building so that each new idea stays where it belongs.")
SEED_TREE_NOTE = X("04", "A and B appear under all three seeds, and D and E under two. They are the hubs; the others each answer one assumption.")
SEED_K_NOTE = X("04", "capability security (unopened)")

# --------------------------------------------------------------------------
# Does the box hold? (round 2, and the 02b branch)
# --------------------------------------------------------------------------

BOX_SHORT = [
    X("02", "**Does the box hold?** As a way to look at one level from above, yes. For the store as it is built, not yet. It is two levels in one box, and a Rama word shows on its outside."),
    X("02", "**What are we building on?** A shared store of facts. Every write is an act by someone, in a layer, under a permission. Every read goes through a list of layers, nearest first. Rama was the floor for building that. From here up, nobody should need a Rama word."),
    X("02", "**Should the system be built in abstraction levels?** Yes. But the cause of the spreading goes deeper than Rama against Softland, and I would not draw every level up front."),
]
BOX_HOLDS = X("02", "**What holds.** Seen from above, a level should be a box. You know what goes in, what comes out, and what it promises. You never need the inside.")
BOX_NOT = [
    {
        "lead": X("02", "**It is not one box.** The store as built is two levels fused together (my reading of the code):"),
        "more": [X("02", "*facts*: acts, facts, their order, and reads once or live;"),
                 X("02", "*layers and permissions*: who may see or act where, and which layers a read goes through."),
                 X("02", "These two change for different reasons. The October decisions that did not hold are all in the second one.")],
    },
    {
        "lead": X("02", "**Its wall leaks.** Every act-id is `[layer depot uuid]`, and the depot is `:streaming` or `:microbatch`, Rama's two kinds of topology (checked, `act.clj`)."),
        "more": [X("02", "`replaces`, `based-on` and `because-of` are all built from act-ids, so a Rama routing word sits inside every reference to an act or a fact. Until 9 October the word was \"class\", a Softland word.")],
        "depot": True,
    },
    {
        "lead": X("02", "**It must open.**"),
        "more": [X("02", "Abstraction means you don't *have to* look inside; it must never mean you *can't*. Clean levels help here, because each level becomes a place to stop when you zoom into how a thing is made.")],
    },
]

DEPOT = {
    "r2": X("02", "Every act-id is `[layer depot uuid]`, and the depot is `:streaming` or `:microbatch`, Rama's two kinds of topology (checked, `act.clj`). `replaces`, `based-on` and `because-of` are all built from act-ids, so a Rama routing word sits inside every reference to an act or a fact. Until 9 October the word was \"class\", a Softland word."),
    "r2_honest": X("02", "\"Class\" was a word of that kind, and \"depot\" made it Rama's."),
    "b2": X("02b", "A correction to my last reply comes first. I said the depot words carried Rama into the act-id. The 6 October decisions show that `:streaming` and `:microbatch` are words you decided yourself (\"my streaming and microbatch\"), and that day's notes already priced what changing them would cost. \"Class\" was the slot's name until 9 October, when it became \"depot\". So the question is narrower than I made it: do those two words name a promise (how soon a layer's acts are decided) or a mechanism (which Rama topology decides them)? As promise words they are fine."),
    "b2_knob": X("02b", "And `:streaming` and `:microbatch`, your words, fit well as the store's own promise words for Knob B, as long as they are defined by what they promise and not by which topology runs them."),
    "recur_A": X("03", "The depot inside every act-id is the same kind of thing: right for today's Rama module, and copied into every reference that is ever made."),
    "recur_G": X("05", "Rama is services and the act is site, so the depot name inside every act-id is wiring cast into concrete."),
    "map_warn": X("MAP", "Read it before relying on the depot claim of round 2 and of breakouts D and G."),
    "map_window": X("MAP", "The depot inside every act-id (round 2, D, G). Read 02b's correction first."),
    "shared": X("02b", "Whatever the door puts into an act-id is therefore copied into `replaces`, `based-on` and `because-of` for ever."),
}

SPREAD = [
    {"lead": X("02", "**a. The architecture was drawn as a pipeline.**"),
     "more": [X("02", "A pipeline has no levels. Whatever travels with the data passes through every row: who, identity, time, permission. So every decision about one of those touches every row."),
              X("02", "If you split a program into modules by processing step, every change spreads. If each module hides one decision that is likely to change, changes stay local.")]},
    {"lead": X("02", "**b. One word, three meanings.**"),
     "more": [X("02", "When one word spans levels, a thought about one level slides into the others.")],
     "later": [X("02b", "Last time I called \"layer\" one word with three meanings. Your fractal layer and the store's layer can be **the same concept**: reference and overlay, at every scale. Calling that a collision was wrong."),
               X("03", "Your editor example folds four relations into one word:")]},
    {"lead": X("02", "**c. The waist was asked to do two jobs.**"),
     "more": [X("02", "That answers one question: what is it made of, and who can change it? A level answers a different question: what must I think about to use it? The two answers mostly line up, but not always:")]},
]
SPREAD_INTRO = X("02", "You say the cause is that Rama and Softland were never kept apart. That is true. I think it is one case of three deeper causes.")
SPREAD_QUAL = X("02", "**A qualification.** Someone does have to think in Rama terms: whoever builds the fact level. Keeping them apart means only that person does, and states the result in Softland words.")

LEVEL_PARTS = [
    ("words", X("02", "**Words:** the few nouns and verbs you think in at that level.")),
    ("promises", X("02", "**Promises:** what it guarantees to the level above.")),
    ("secret", X("02", "**A secret:** what it hides, so it can change without anyone above noticing.")),
    ("direction", X("02", "**One direction:** it uses only the words and promises of the levels below it.")),
]
LEVEL_KNOBS = X("03", "So last turn's definition of a level grows by one part: words, promises, secrets, one direction, **and knobs**.")
LEVEL_TESTS = [
    X("02", "**The reasons to change differ.** New storage technology changes the fact level. A new social model changes the permission level."),
    X("02", "**The swap test.** Could you replace what is below without anyone above noticing?"),
    X("02", "**The think test.** Can you work at the level using only its own words?"),
    X("02", "**Pace.** Does the level change at its own speed?"),
]
LEVEL_PROOF = X("02", "A level's promises are exactly what a proof is about, and that proof may assume only the promises of the levels below.")
LEVEL_AGENT = X("02", "An agent is not a level; it is an inhabitant that uses the interfaces.")

DISAGREE = [
    {"lead": X("02", "**Do not design every level up front.**"),
     "more": [X("02", "The **bottom levels** hold data forever. Design them now, carefully, with proofs."),
              X("02", "The **top levels** are records, cheap to change. Let use show where the seams are.")]},
    {"lead": X("02", "**The act is the slowest thing in the system.**"),
     "more": [X("02", "After the first real record, every slot in the act's form stays in the record forever."),
              X("02", "So ask one question of each slot: does the store check it, or only carry it? What the store checks belongs in the form. What it only carries could be content instead: facts inside the act, owned by the level that needs them.")]},
    {"lead": X("02", "**Hide Rama's words, not its strengths.**"),
     "more": [X("02", "An interface that any database could fill would throw away the reasons Rama was chosen: computation next to the data, live reads, and a store that comes out the same when it is run again. Keep those, and state them as promises in Softland words.")]},
    {"lead": X("02", "**Some leaks are honest.**"),
     "more": [X("02", "A person who is typing cares whether an act is decided at once or in a batch. Do not hide that difference. State it as a promise in the upper level's words, such as \"decided at once\" or \"decided in a batch\".")],
     "pasted_back": X("MAP", 'It holds one paste of the session\'s own text ("Some leaks are honest ..."), which is not his.')},
]
WHY_NOW = X("02", "Before the first real record is kept, the bottom levels can be redrawn for free. After it, they cannot.")

# --------------------------------------------------------------------------
# The stack.
# --------------------------------------------------------------------------

STACK_INTRO = X("MAP", "Each level is defined by the question it lets the levels above stop asking. The levels were derived from the vision and the settled decisions, not from the code.")
STORE_AS_BUILT = X("MAP", "It is levels 1 to 5 fused (the session's reading of the code). Its interface, the act and the read, is what everything above builds on.")
MATERIAL_AXIS = X("MAP", 'It runs beside the stack, in Sid\'s 30 July words: "three strata — material, Softland code, and host floor". The two axes cross; neither replaces the other.')
MATERIAL_HOW = X("05", "Material holds the cases of every level. Softland code holds the engines of levels 1–6. The host floor is under level 1.")
STRATA_QUOTE = X("05", "three strata — material, Softland code, and host floor")
WAIST_LINE = X("MAP", "the waist runs through 6: the executor is code, records are data")
WAIST_R2 = X("02", "**The waist runs through computation.** The executor is code and records are data. The material of the lower levels (facts, permission grants) is data too. So the waist is a line among engines; it does not split the levels.")

# Each level: name and question as MAP's stack draws them; the rest from
# breakout B (round 3 for levels 1 to 7, round 5 for 8 to 11).
LEVELS = {
    1: {"name": "Log", "q": X("MAP", "did it happen, and in what order?"), "src": "03",
        "settles": X("03", "*did it happen, and in what order?*"),
        "words": X("03", "entry, append, order, replay."),
        "promises": [X("03", "an entry is never lost or changed; each line has one order; an entry is all or nothing; replaying gives the same result.")],
        "from": X("03", '"Rama is truth … no truth outside the log" (decisions.md), and "The return is the trust moment" (carry-on §4).')},
    2: {"name": "Facts", "q": X("MAP", "what is true, now and then?"), "src": "03",
        "settles": X("03", "*what is true, now and at any earlier time?*"),
        "words": X("03", "fact, entity, key, value, replace, as-of."),
        "promises": [X("03", "the state is exactly what replaying the log gives, and history is never lost.")],
        "from": X("03", 'your act form of 6 October, and "Rows are truth and derived rows" (decisions.md).'),
        "editor": X("03", "the text is facts;")},
    3: {"name": "Live", "q": X("MAP", "when it changes, how do I learn?"), "note": X("MAP", "(may fold into 2)"), "src": "03",
        "settles": X("03", "*when something changes, how do I learn of it?*"),
        "words": X("03", "watch, change, push."),
        "promises": [X("03", "every change to what I watch reaches me in order, or I am told that I missed some.")],
        "from": X("03", '"redraw that part while the rest stays constant" (carry-on §7), and the render boundary\'s diagnosis, "the read side paying to rediscover what the write side already knew". This level could also fold into facts.'),
        "editor": X("03", "the cursor is live;")},
    4: {"name": "Layers", "q": X("MAP", "which world am I in?"), "src": "03",
        "settles": X("03", "*which world am I in, and where do my changes go?*"),
        "words": X("03", "layer, builds on, nearest, base, fork, compare, adopt."),
        "promises": [X("03", "my write changes no read that does not go through my layer; the nearest layer wins; adoption happens all at once.")],
        "from": X("03", '"An active layer is work against a base" (carry-on §2), and "Candidates are real, and adoption has scope" (§5).'),
        "editor": X("03", "my config is a layer;")},
    5: {"name": "Standing", "q": X("MAP", "who did this, and were they allowed?"), "src": "03",
        "settles": X("03", "*who did this, and were they allowed?*"),
        "words": X("03", "who, permission, grant, see, act."),
        "promises": [X("03", "nothing lands without a permission that allows it, and every permission ties back to one person.")],
        "from": X("03", '"what am i allowed to query" (carry-on §9), and the store\'s decisions of 8 October.'),
        "editor": X("03", "who may see my config is standing;"),
        "extra": [X("03", '**Standing sits above layers, but "who" runs through everything.**')]},
    6: {"name": "Computation", "q": X("MAP", "how is one thing made from another?"), "src": "03",
        "settles": X("03", "*how is one thing made from another, and can I trust the result?*"),
        "words": X("03", "record, step, capability, run, budget."),
        "promises": [X("03", "every run ends; the same inputs give the same output; every result keeps what it was made from.")],
        "from": X("03", '"Tools are records over a vocabulary" (decisions.md), and the ink outline as "a deterministic, versioned derivation" (carry-on §7).'),
        "editor": X("03", "syntax colouring is computation;")},
    7: {"name": "Meaning", "q": X("MAP", "what is this, what does it mean?"), "note": X("MAP", "(key meaning may sit lower)"), "src": "03",
        "settles": X("03", "*what is this, and what does it mean?*"),
        "words": X("03", "kind, name, key meaning, grammar."),
        "promises": [X("03", "every key in use has a meaning you can read, and a name resolves through your layers.")],
        "from": X("03", '"vocabularies should themselves become changeable, forkable, inheritable specifications" (carry-on §3).'),
        "editor": X("03", "what a keymap *is* is meaning."),
        "extra": [X("03", '**Meaning splits in two.** "What does this key mean?" must stay answerable after the code that wrote the fact is gone. So key meaning sits low and changes slowly; Datomic, for example, keeps its schema as ordinary facts. "What kinds of thing exist, what are they called, and where are they?" is a catalog, which sits higher and changes faster.')]},
    8: {"name": "Change", "q": X("MAP", "what changed, and which state counts?"), "src": "05",
        "settles": X("05", "*what changed, between which two states, and which state counts?*"),
        "holds": [X("05", '**Named versions.** "This layer as of this stamp", written as a fact, like a git tag.'),
                  X("05", "**Candidates.** A layer that proposes changes to a base."),
                  X("05", "**Diffs.** A structured difference between two states of the same things."),
                  X("05", "**Adoption and reversal.** One act makes a candidate the default, and one act undoes it.")],
        "from": X("05", 'Carry-on §2 separates two needs: "Automatic event history and an explicit meaningful version are different needs." The log already gives the first one; this level is the second.'),
        "extra": [X("05", 'Without Meaning, a diff can only say "these facts differ".'),
                  X("05", "So Change splits: naming a state is low, and comparing states is high."),
                  X("05", "**Identity is the hard core.**")]},
    9: {"name": "Surfaces", "q": X("MAP", "how do I see it and act on it?"), "src": "05",
        "settles": X("05", "*how do I see this at the level that helps me act, and how does my gesture become a change?*"),
        "halves": [X("05", "**Get:** facts become something to look at."),
                   X("05", "**Put:** a gesture becomes acts.")],
        "promises": [X("05", "What is shown is a function of facts, with no hidden state."),
                     X("05", 'Everything shown can say which facts it came from (carry-on §3, "Context should follow attention").'),
                     X("05", "An edit through a view lands as acts, and the view shows the committed result.")],
        "extra": [X("05", "A view is right only if it is true *and* at the right level.")]},
    10: {"name": "Components", "q": X("MAP", "what is it made of; how do I make a variant?"), "src": "05",
         "settles": X("05", "*what is this thing made of, how is it made, and how do I make another one, or a variant?*"),
         "words": X("05", "Words: entity, component (data on an entity), template (the editable composition behind a live thing), instance, override, variant, part, binding, and system (behaviour that runs over entities with certain components)."),
         "promises": [X("05", "An instance shows its template's composition plus its own overrides. This is delegation."),
                      X("05", "A part is itself a component, to any depth. This is containment."),
                      X("05", "A template change reaches an instance only through a version that the instance follows.")],
         "extra": [X("05", '**The discomfort with "type" goes away here.**'),
                   X("05", "**Finding: Surfaces and Components may be one level.**")]},
    11: {"name": "Work", "q": X("MAP", "what are we doing, why, what is settled?"), "src": "05",
         "settles": X("05", "*what are we doing, why, what is settled, and what is next?*"),
         "words": X("05", "Words: question, claim, evidence, source (the discourse-graph base named in CLAUDE.md), trail, briefing, decision, working, settled."),
         "promises": [X("05", "Every claim can show its evidence and sources."),
                      X("05", "Every decision can show what it rests on."),
                      X("05", "Tentative and settled things look visibly different.")],
         "extra": [X("05", "This level uses every level below it")]},
}

# Places where a round's own sentence ties a breakout, bridge or tension to a
# level. ("T2" means tension 2 of MAP; "06-1" means bridge 1 of round 6.)
TOUCH = {
    1: [("C", X("03", "The log is truth.")),
        ("G", X("05", "the act's form; the log's promises (order, atomicity, durability)")),
        ("T2", None)],
    2: [("C", X("03", "The current state is a view.")),
        ("J", X("05", "So an entity-key-value store *is* ∫I.")),
        ("T2", None)],
    3: [("B", X("03", "This level could also fold into facts.")),
        ("J", X("05", "Δ is easy to keep live, because it only re-indexes."))],
    4: [("H", X("05", "A read through a list of layers, nearest first, is a union directory searched in order")),
        ("C", X("03", "Layer resolution (nearest wins) is a view.")),
        ("E", X("05", "Softland could offer both grains as a knob on builds-on: by place or by part.")),
        ("T8", None),
        ("words", X("MAP", '"base" lives on three levels: a layer others build on (4), the system\'s layer that everyone reads (5), and what is accepted (11).')),
        ("words", X("MAP", '"system" spans levels 4, 5 and 6.'))],
    5: [("K", X("05", "A permission id is `[who layer granted-in parent allows]`: a chain of grants back to a first permission held by a person, each of which can narrow to `:see`.")),
        ("M", X("05", "Share-alike is a rule at the Standing level that the gate can check")),
        ("I", X("05", "Softland's permission model ties every grant to one person, which gives each layer its own Linus.")),
        ("T11", None), ("T15", None),
        ("words", X("MAP", '"base" lives on three levels: a layer others build on (4), the system\'s layer that everyone reads (5), and what is accepted (11).')),
        ("words", X("MAP", '"system" spans levels 4, 5 and 6.'))],
    6: [("D", X("03", "the record language, with records above and runners below;")),
        ("A", X("03", "**Records are like eBPF, not like processes.**")),
        ("K", X("05", "A converter that the system runs on your behalf must use *your* permission (the one the asker names), never the system's.")),
        ("E", X("05", "So infinite layers do not mean infinite cost: layers on layers can be compiled through and run at the speed of one.")),
        ("words", X("MAP", '"system" spans levels 4, 5 and 6.'))],
    7: [("B", X("03", "**Meaning splits in two.**")),
        ("J", X("05", "What it lacks is the C and the π, which are the Meaning level.")),
        ("J", X("05", "That is the rigorous form of B's claim that meaning must sit low.")),
        ("T5", None), ("T12", None)],
    8: [("06-2", X("06", "So level 8 needs no separate machinery: spans diff, pushouts merge, and double pushouts apply.")),
        ("06-2", X("06", "So first-maker decision 3 decides whether level 8 is exact or guessed.")),
        ("I", X("05", "This gives the Change level a vocabulary: each operator is a kind of change, with its own diff and its own risks.")),
        ("H", X("05", "For Softland's Change level, a past state is just another layer you can put in your list."))],
    9: [("06-1", X("06", 'That is a mathematical reason to make Surfaces and Components one level, your "ECS layer".')),
        ("J", X("05", "In Softland, get is the view record and put is the gesture converter (B's Surfaces).")),
        ("H", X("05", "if a panel offers the same interface as the whole canvas (pointer, keys, a surface to draw on, a list of layers), then any Softland workspace can run inside a panel of another one.")),
        ("T10", None)],
    10: [("06-1", X("06", 'That is a mathematical reason to make Surfaces and Components one level, your "ECS layer".')),
         ("E", X("05", "The ECS library Flecs does exactly this with two relations: `IsA` (delegation) and `ChildOf` (containment).")),
         ("E", X("05", "So containment in Softland is a structure with sharing, not a strict tree.")),
         ("T9", None), ("T10", None)],
    11: [("M", X("05", "an ask (a Work fact, with terms);")),
         ("L", X("05", "a Work-level agent sets goals for Computation-level agents, which set goals for theirs.")),
         ("words", X("MAP", '"base" lives on three levels: a layer others build on (4), the system\'s layer that everyone reads (5), and what is accepted (11).'))],
}

THREADS = [
    ("who", X("03", 'Yet every act has a who, even in a world with one person. So "who" is a thread through every level, while "may" is a level of its own.'), "03"),
    ("when", X("03", "On 14 October you put order and time apart: there is order inside a layer, and a time label on every act."), "03"),
    ("why", X("01", "The store already has places for the why (`session`, `because-of`) and for what an act read (`based-on`). The builder's trail needs no new mechanism, only a habit of filling them."), "01"),
    ("about whom", X("05", "That is a fourth thread through every level, beside who, when and why: about whom."), "05"),
]
THREADS_ALL = X("02", '**Who, when and why run through every level.** This is your 13 September picture: "data transfer is like pipes, elcetricty cables". No level owns them, and every level carries them.')

# How the stack grew, round by round (names as each round wrote them).
GREW = [
    {"src": "02", "label": "Round 2", "alt": False,
     "levels": ["facts", "layers & permissions", "computation  (next)", "open, found by use"],
     "boundary": (2, X("02", "the box's interface: act + read")),
     "through": "who · when · why",
     "caption": X("02", "This is a proposal: firm at the bottom, open at the top.")},
    {"src": "02b", "label": "02b, alt branch", "alt": True,
     "levels": ["S0 · The log", "S1 · Decisions", "S2 · Facts", "S3 · Layers", "S4 · Standing", "S5? · Vocabulary"],
     "boundary": None, "through": "who, when, why",
     "caption": X("02b", "Many: five clear levels (S0 to S4), one candidate (S5), a sibling (blobs) and an edge (the uncommitted).")},
    {"src": "03", "label": "Round 3, breakout B", "alt": False,
     "levels": ["Log", "Facts", "Live", "Layers", "Standing", "Computation", "Meaning"],
     "boundary": None, "through": "who, when and why",
     "caption": X("03", "Last turn I said two. The derivation finds more seams than the code shows.")},
    {"src": "05", "label": "Round 5, B continued", "alt": False,
     "levels": ["Log", "Facts", "Live", "Layers", "Standing", "Computation", "Meaning", "Change", "Surfaces", "Components", "Work"],
     "boundary": (5.5, X("05", "the waist runs through 6: the executor is code, records are data")),
     "through": "who · when · why · about whom",
     "caption": X("05", "Going higher, four more levels appear, and two of them may not separate cleanly.")},
    {"src": "06", "label": "Round 6", "alt": False,
     "levels": ["Log", "Facts", "Live", "Layers", "Standing", "Computation", "Meaning", "Change", "Surfaces + Components", "Work"],
     "boundary": (5.5, None),
     "through": "who · when · why · about whom",
     "caption": X("06", "**Tension 4 (Surfaces or Components):** one level, on Poly's grounds. That is my reading, and the store still supplies identity and persistence.")},
]
GREW_NAME_CHECKS = [X("02", "open, found by use"), X("02", "layers & permissions"), X("02", "computation  (next)"),
                    X("02b", "**S0 · The log"), X("02b", "**S1 · Decisions"), X("02b", "**S2 · Facts"),
                    X("02b", "**S3 · Layers"), X("02b", "**S4 · Standing"), X("02b", "**S5? · Vocabulary")]
GREW_BESIDE = X("02b", "**Beside the store: blobs.**")
GREW_BEFORE = X("02b", "**Before the store: the uncommitted.**")

# The waist asked to do two jobs (round 2, section 2c), and D's hourglasses.
TWO_LINES = {
    "sid": X("02", "i use waist as to refer to something that has to exist as code vs what can and should be build on top"),
    "q_made": X("02", "what is it made of, and who can change it?"),
    "q_level": X("02", "what must I think about to use it?"),
    "grant": X("02", "A permission grant is low-level, but it is data."),
    "grammar": X("02", "The grammar reader is code, but it serves a high-level need: formats."),
    "keymeaning": X("02", "The meaning of a key sounds high-level, but it must last as long as the facts that use it."),
    "lined_up": X("02", "In rendering the two questions lined up, so it worked there. After that, the waist stood in for levels everywhere, and everything below it became one region."),
    "engines": X("05", "Softland code holds the engines of levels 1–6."),
    "records": X("02", "The executor is code and records are data."),
    "lower_data": X("02", "The material of the lower levels (facts, permission grants) is data too."),
}
HOURGLASSES = [
    (X("03", "the act, with makers above and storage below;"), "the act", "makers", "storage"),
    (X("03", "the record language, with records above and runners below;"), "the record language", "records", "runners"),
    (X("03", "the builtins, with geometry generators above and the GPU below;"), "the builtins", "geometry generators", "the GPU"),
    (X("03", "the grammar language, with formats above and one reader below."), "the grammar language", "formats", "one reader"),
]
HOURGLASS_SO = X("03", 'So the waist is the narrow language in the middle, not just "code versus data".')

# --------------------------------------------------------------------------
# The word "layer".
# --------------------------------------------------------------------------

LAYER_SID = X("MAP", "i think i use the word layer as a abstraction itself like a layer can have layers in itself for e.g editor is a layer then code editor and markdown editor and text editors are also a layer in itself and then each layer then has their own layer e.g equivalent of vim, vscode, emacs ... and then a user can combine differents parts from different ones and then its their editor layer which can then be further taken by other users and built on top ... yeah and this is one example an editor in itself is a layer ... fractal to the bottom to the top ... i think like this and thus maybe my vocab also makes is harder for my self and you ....")
KNOBS_SID = X("MAP", "yeah its giving the users the spectrum and control over the system some knobs do require to be available on different layers/levels imo")
LAYER_MAP = X("MAP", "It is the unit of composition that repeats at every level above the waist; \"level\" names only the step of abstraction.")
RELATIONS = [
    ("kind", "Kind of", X("03", "**Kind of.** A code editor is a kind of editor, and a vim-like editor is a kind of code editor."), "delegation"),
    ("built", "Built on", X("03", "**Built on.** My editor builds on the vim-like and the vscode-like editors and overrides parts of each."), "delegation"),
    ("made", "Made of", X("03", "**Made of.** An editor is made of a buffer, a keymap, a renderer and a command list."), "containment"),
    ("words", "Words for", X("03", "**Words for.** The editor layer gives words (buffer, cursor, mode, keymap), and the vim-like layer is written in those words."), "translation"),
]
RELATIONS_TROUBLE = X("03", 'The trouble is that one word carries four relations, so "a layer on top of the editor layer" can mean any of the four.')
RELATIONS_ONE = X("03", "The store's builds-on with nearest-wins is delegation. Relation 3 is a different relation, containment. Relation 4 is where a layer becomes a platform.")
ALGEBRAS = [
    ("delegation", "a monoid", X("05", "**Delegation** (kind-of and built-on) is a lookup that goes up a chain of parents, and the first parent with a value wins."),
     X("05", "At each place, that is the \"first\" monoid: take the first defined value, folded along the list.")),
    ("containment", "an operad", X("05", "**Containment** (made-of) is assembly: a whole is made of parts, and each part is made of parts."),
     X("06", "**Transclusion is the diagonal, placed after one state.**")),
    ("translation", "a functor", X("05", "**Words-for** (one layer gives the words that another is written in) is a translation between vocabularies, which is a functor (J)."),
     X("05", "**Δ is a view.**")),
]
ALGEBRAS_WHY = X("05", 'Each of the three is recursive, so each is fractal on its own. That is why one word, "layer", felt right: all three are fractal. It is also why one mechanism strains: they are different algebras with different laws.')
INTERACT = [
    X("05", "**Delegation lifts through containment.**"),
    X("05", "**Overrides are addressed by containment paths.**"),
    X("05", "**The identity problem comes back.**"),
    X("05", "**The half-part problem is a choice of grain.**"),
    X("05", "**Multiple parents need a policy.**"),
    X("05", "**Interaction walks both relations, in order.**"),
    X("05", "**Things move between the two relations over time.**"),
    X("05", "**Containment is not a tree.**"),
]
LAWS = [
    (X("06", "**Override commutes with zoom.**"), X("06", "`part(L₁ ⊕ L₂) = part(L₁) ⊕ part(L₂)`")),
    (X("06", "**Translation preserves nesting.**"), X("06", "`F` sends parts to parts and nested parts to nested parts; in other words, it is an operad morphism.")),
    (X("06", "**Translation commutes with override, for views.**"), X("06", "`Δ_F(L₁ ⊕ L₂) = Δ_F(L₁) ⊕ Δ_F(L₂)` always holds. For Σ and Π it holds only when `F` merges no places.")),
    (X("06", "**Late binding.**"), X("06", "Inside a whole, a part's reference to a sibling resolves through the final world, not through the parent's. This is the law that Nix adds.")),
]
LAWS_SO = X("06", 'When all four hold, you can zoom, translate and override in any order and get the same result. That is the "think test" made exact: anyone, person or agent, can reason at one level without checking the others.')
ALT_PRIMS = [
    X("02b", "**Reference.** A fact points at other things. This gives \"contains\" and \"picks from\"."),
    X("02b", "**Overlay.** A layer builds on an ordered list, and the nearest wins. This gives \"is a kind of\", \"mixes in\", \"overrides\" and \"builds on\"."),
]
ALT_SAME = X("02b", "Your fractal layer and the store's layer can be **the same concept**: reference and overlay, at every scale.")
ALT_RUNS = X("02b", "Layers are many, fractal and fast. Levels are few, stacked and slow.")
ALT_BRIDGE = X("02b", "The fractal reaches into every level **through its knobs, never through its engine**.")

# --------------------------------------------------------------------------
# Breakouts A to M, the three bridges of round 6, and the 02b threads.
# --------------------------------------------------------------------------

OUTSIDE = "outside"   # rests on outside references (MAP: the session's knowledge)
DERIVED = "derived"   # the session's own reasoning

BREAKOUTS = [
    {"id": "A", "rounds": ["03"], "work": [("03", "A. The computer's levels: each level a machine for the one above")],
     "kinds": [OUTSIDE, DERIVED],
     "framing": X("03", "Tanenbaum describes a computer as a stack of virtual machines: digital logic, microarchitecture, instruction set, operating system, assembly, high-level languages. Each level has its own language."),
     "shows": [X("03", "**The instruction set is the boundary that matters most.**"),
               X("03", "**A leak into the instruction set lasts forever.**"),
               X("03", "**Levels can be hybrid.**"),
               X("03", "**Softland may need an operating-system level.**"),
               X("03", "**Records are like eBPF, not like processes.**")],
     "editor": [X("03", "an application on Softland's operating system. When it offers words like buffer and keymap to other layers, it is also a platform.")],
     "pulls": X("03", "a computer's levels below the instruction set are fixed silicon, but Softland wants to open them from inside (\"The loop should eventually include itself\", carry-on §5).")},
    {"id": "B", "rounds": ["03", "05"], "work": [("03", "B. The levels Softland needs, derived from the log up"), ("05", "B, continued: the levels above Meaning")],
     "kinds": [DERIVED],
     "framing": X("03", "I start from the vision and the settled decisions, not from the code. Each level is defined by the question it lets the levels above stop asking."),
     "shows": [X("03", "**The store as built is five of these levels fused:** log, facts, live, layers and standing (my reading of the code)."),
               X("03", "**Order needs a scope.**"),
               X("03", "**Meaning splits in two.**"),
               X("03", "**The gaps are the work.**"),
               X("05", "Going higher, four more levels appear, and two of them may not separate cleanly."),
               X("05", '**Finding: "base" lives on three levels.**')],
     "editor": [X("03", "the text is facts;"), X("03", "the cursor is live;"), X("03", "my config is a layer;"),
                X("03", "who may see my config is standing;"), X("03", "syntax colouring is computation;"),
                X("03", "what a keymap *is* is meaning.")],
     "pulls": X("03", "seven levels found by derivation can still be wrong. Each one has to pass last turn's four tests (different reasons to change, the swap test, the think test, pace) before it hardens.")},
    {"id": "C", "rounds": ["03"], "work": [("03", "C. The store inside out: one truth, many views")],
     "kinds": [OUTSIDE, DERIVED],
     "framing": X("03", "Martin Kleppmann's talk \"Turning the database inside out\" says that the log *is* the database. Tables, indexes and caches are views computed from the log."),
     "shows": [X("03", "So every table is a view of the log."),
               X("03", "The log is truth. The current state is a view. Layer resolution (nearest wins) is a view."),
               X("03", "If layer resolution is a view, a person could one day define a new kind of layer as a record (\"the majority of these three layers wins\") without changing the store's code. The waist would move down one level.")],
     "editor": [X("03", "the document is a view of the log, undo is the log read backwards, and \"my view of the document\" is a view with my overrides.")],
     "pulls": X("03", "some checks have to happen at write time. If anyone could write into the base and only a view ignored it, the base's log would fill with junk.")},
    {"id": "D", "rounds": ["03"], "work": [("03", "D. The hourglass: the narrow waist")],
     "kinds": [OUTSIDE, DERIVED],
     "framing": X("03", "The internet is drawn as an hourglass: many networks below, many applications above, and one narrow level in the middle, IP."),
     "shows": [X("03", "**Your word \"waist\" may mean this.**"),
               X("03", "**There are several waists, one at each boundary that matters:**"),
               X("03", "**The act is a waist, and it is getting fat.**")],
     "editor": [X("03", "every editor ever made meets the store through the act, so the act should never know what an editor is.")],
     "pulls": X("03", "Softland's store is not a best-effort pipe. It is the shared truth, with permissions, so a waist with guarantees is fatter than IP by design. How fat it should be is the open question.")},
    {"id": "E", "rounds": ["03", "05"], "work": [("03", "E. Your fractal: every layer can become a platform"), ("05", "E, continued: towers, reflection, and containment alongside delegation")],
     "kinds": [OUTSIDE, DERIVED],
     "framing": X("03", "Each one is an application of the level below and a platform for the level above. Your editor example is this tower, plus composition among siblings."),
     "shows": [X("03", "**A and E fit together.**"),
               X("03", "**Emacs is your editor example, already lived.**"),
               X("03", '**"Fractal to the bottom" has a floor.**'),
               X("05", "**Your words already describe a reflective tower.**"),
               X("05", "**Three algebras, not one mechanism.**"),
               X("05", "**Containment is not a tree.**")],
     "editor": [X("05", "Your editor `IsA` your-tweaks, which `IsA` vim-like."),
                X("05", "Your command palette is transcluded from vscode-like's, so the same part sits inside two wholes.")],
     "pulls": X("03", "But putting four relations into one mechanism may force different needs into one shape. Whether to keep it uniform or split it is open.")},
    {"id": "F", "rounds": ["03"], "work": [("03", "F. Your knobs: control across levels")],
     "kinds": [OUTSIDE, DERIVED], "sid": "knobs",
     "framing": X("03", "A level hides its mechanism, but it offers knobs: chosen points on a spectrum, named in its own words."),
     "shows": [X("03", "**Name each knob where the person who cares about it lives.**"),
               X("03", "**Each knob has a grain:**"),
               X("03", "**Knobs can be facts in layers.**"),
               X("03", "**Every setting must be a valid world.**"),
               X("03", "**Whoever is affected can read the setting.**")],
     "editor": [X("03", "typing is \"at once\", and a refactor across 500 files is \"in a batch, all or nothing\". It is one editor with two knob settings.")],
     "adds": X("03", "So last turn's definition of a level grows by one part: words, promises, secrets, one direction, **and knobs**.")},
    {"id": "G", "rounds": ["03", "05"], "work": [("03", "G–J. Shorter breakouts"), ("05", "G. Pace: how a system learns without tearing")],
     "kinds": [OUTSIDE, DERIVED],
     "framing": X("05", "The layers \"shear\", sliding past each other at their own speeds. A building that learns lets them slide. A building that locks them together tears when it changes."),
     "shows": [X("05", "**The derived levels have paces.**"),
               X("05", "**Pace is earned, not assigned.**"),
               X("05", "**Every pace boundary needs three mechanisms.**"),
               X("05", "**Editions are how the forever layer can still change.**"),
               X("05", "**Proof effort should follow pace.**")],
     "editor": [X("05", "Your tweaks can change every minute because nothing builds on them. The vim-like template slows down as people adopt it. The act never changes.")],
     "pulls": X("05", "So is pace a property of a layer, or of each builds-on edge? Possibly the edge.")},
    {"id": "H", "rounds": ["03", "05"], "work": [("03", "G–J. Shorter breakouts"), ("05", "H. Plan 9: everyone composes their own world")],
     "kinds": [OUTSIDE, DERIVED],
     "framing": X("05", "The second idea is the one that matters here: **every process has its own namespace**, its own view of the file tree, built from parts."),
     "shows": [X("05", "**The store's reads are Plan 9 unions.**"),
               X("05", "**A namespace is data.**"),
               X("05", "**Plan 9's unions are shallow, on purpose.**"),
               X("05", "**Using the same interface at every scale is what makes nesting work.**"),
               X("05", "**Time travel was a namespace.**")],
     "editor": [X("05", "your editor's world is a namespace: vim-like before vscode-like before base, with your tweaks layer marked `-c` so that your changes land there.")],
     "pulls": X("05", "That is more than Plan 9 had, and harder: who may change the list of a shared world?")},
    {"id": "I", "rounds": ["03", "05"], "work": [("03", "G–J. Shorter breakouts"), ("05", "I. Conway's law and Linux: the social shape of layers")],
     "kinds": [OUTSIDE, DERIVED],
     "framing": X("05", "The architecture ends up shaped like who talks to whom."),
     "shows": [X("05", "**Linux is the pace split, enforced socially.**"),
               X("05", "**The six operators are what people will do with layers.**"),
               X("05", "**Conway's law becomes computable in Softland.**"),
               X("05", "**Agents are part of the organization.**"),
               X("05", "**The inverse move.**")],
     "editor": [X("05", "the vim-like template has a maintainer, recorded as a `MAINTAINERS`-style fact.")],
     "pulls": X("05", "Linux has one Linus. Softland's permission model ties every grant to one person, which gives each layer its own Linus. That works for a project and fails for a commons, through bus factor, burnout and capture.")},
    {"id": "J", "rounds": ["03", "05"], "work": [("03", "G–J. Shorter breakouts"), ("05", "J. The math of translating between vocabularies")],
     "kinds": [OUTSIDE, DERIVED],
     "framing": X("05", "In it, a vocabulary is a category, data in it is a functor, and a translation between vocabularies gives exactly three ways to move data."),
     "shows": [X("05", "**flattening typed data into facts loses nothing if, and only if, you keep the vocabulary and each thing's kind.**"),
               X("05", "**Δ is a view.**"),
               X("05", "**Σ pushes forward by gluing.**"),
               X("05", "**Π pushes forward by matching.**"),
               X("05", 'So the question "is this vocabulary change breaking?" becomes "is there a fully faithful F : Old → New?"'),
               X("05", "So the math separates **integration**, which loses nothing and keeps everything, from **resolution**, which is a policy you can change.")],
     "editor": [X("05", "Your editor is their pushout plus your resolution policy."),
                X("05", "Upgrading the editor template is a migration, and whether it is safe is a theorem you can check.")],
     "pulls": X("05", "The math wants declared vocabularies, a category for each layer, while the store's keys are open and anyone can write any key.")},
    {"id": "K", "rounds": ["05"], "work": [("05", "K. Capability security: authority you hold, not authority you are")],
     "kinds": [OUTSIDE, DERIVED],
     "framing": X("05", "**Capabilities:** you can act on a thing only if you hold a reference that designates it and carries the right to act. Authority is what you hold."),
     "shows": [X("05", "**Softland is already halfway to capabilities, and it has the better half.**"),
               X("05", "**The resident builder needs least authority from its first act.**"),
               X("05", "**Revocation is the safety valve for agents.**"),
               X("05", "**Records should carry no authority of their own.**"),
               X("05", "**Bearer or bound.**"),
               X("05", "**One store holds all the grants, so they can be audited.**")],
     "editor": [X("05", "A plugin someone else wrote runs with nothing except what you hand it. It can read your keymap layer, cannot see your notes, and stops working the moment you revoke it.")],
     "pulls": X("05", "Bound grants need identity infrastructure for agents (keys, sign-in), which is exactly the work that was reverted on 15 October.")},
    {"id": "L", "rounds": ["05"], "work": [("05", "L. Biology: levels of organization that evolved to be evolvable")],
     "kinds": [OUTSIDE, DERIVED],
     "framing": X("05", "Life is the oldest layered system, and nobody designed it."),
     "shows": [X("05", "**The genetic code is a waist that froze.**"),
               X("05", "**The waist is where checking is strongest.**"),
               X("05", "**Membranes are gates.**"),
               X("05", '**Horizontal gene transfer is "combine different parts from different ones".**'),
               X("05", "**One record, many readings: gene expression is knobs.**"),
               X("05", "**The bow tie.**")],
     "editor": [X("05", "Taking vim's keymap into your editor is horizontal transfer, and it works because the waist is shared.")],
     "pulls": X("05", "Evolution is wasteful, slow and blind, and it has no undo, while Softland is designed, versioned and reversible. The analogies can flatter.")},
    {"id": "M", "rounds": ["05"], "work": [("05", "M. Economics: who pays for a layer others build on")],
     "kinds": [OUTSIDE, DERIVED],
     "framing": X("05", "Layers that others build on are public goods: using one does not use it up, and it is hard to stop people from using it. Economics predicts that public goods are underfunded."),
     "shows": [X("05", "**Attribution is already computed, and credit can be too.**"),
               X("05", "**Goodhart's law is the risk the vision already named.**"),
               X("05", "**What a fork owes its parent can be a fact on the layer.**"),
               X("05", "**Commissioned work is a path through the stack.**"),
               X("05", "**Compute already has a meter.**"),
               X("05", "**Payment can poison a gift economy.**")],
     "editor": [X("05", "The vim-like template that thousands build on has a maintainer who carries its weight.")],
     "pulls": X("05", "money needs identity, law and payment rails, all of which sit outside the store, and every metric that becomes money gets gamed.")},
]

BRIDGES = [
    {"id": "06-1", "n": 1, "short": "sharing; 9 and 10 as one",
     "title": X("06", "1. Sharing, and one level for surfaces and components"),
     "work": [("06", "1. Sharing, and one level for surfaces and components")],
     "from": ["B", "E"], "kinds": [OUTSIDE, DERIVED],
     "asked": X("06", "does that unify surfaces and components into one level and give us the exact algebra for transclusion?"),
     "answer": X("06", "**Short answer: Poly makes levels 9 and 10 one level, and it gives the exact algebra for transclusion. Upgrading to a cartesian operad does not, on its own.**"),
     "shows": [X("06", "**Why the cartesian upgrade is not enough.**"),
               X("06", "**Why Poly works.**"),
               X("06", "**Transclusion is the diagonal, placed after one state.**"),
               X("06", "**What stays open:** an override made inside one whole.")],
     "store": X("06", "In the store, that is two wholes pointing at one entity, versus a new entity that delegates to the old one.")},
    {"id": "06-2", "n": 2, "short": "spans and pushouts",
     "title": X("06", "2. Diffs as spans, merges as pushouts, and chains that collapse"),
     "work": [("06", "2. Diffs as spans, merges as pushouts, and chains that collapse")],
     "from": ["B", "E", "J"], "kinds": [OUTSIDE, DERIVED],
     "asked": X("06", "shouldn't we pair that with a diff in level 8 being a span (I <- U -> J) over the common unchanged core U"),
     "answer": X("06", "**Short answer: yes, to both questions.**"),
     "shows": [X("06", "**Change becomes one algebra.**"),
               X("06", "**Finding `U` is the identity problem.**"),
               X("06", "**Chains of migrations collapse into one.**")],
     "store": X("06", "So first-maker decision 3 decides whether level 8 is exact or guessed.")},
    {"id": "06-3", "n": 3, "short": "laws; Nix overlays",
     "title": X("06", "3. Coherence laws, and Nix overlays in your editor"),
     "work": [("06", "3. Coherence laws, and Nix overlays in your editor")],
     "from": ["E", "J"], "kinds": [OUTSIDE, DERIVED],
     "asked": X("06", "what are the exact interaction laws between our 3 algebras"),
     "answer": X("06", "When all four hold, you can zoom, translate and override in any order and get the same result."),
     "shows": [X("06", "**Override commutes with zoom.**"),
               X("06", "**Translation preserves nesting.**"),
               X("06", "**Translation commutes with override, for views.**"),
               X("06", "**Late binding.**"),
               X("06", "**The grain knob is extend-or-replace.**"),
               X("06", "**A cycle becomes a refusal.**")],
     "store": X("06", "The one rule to make explicit is that a record must never pin its references to siblings to its own home layer.")},
]
BRIDGE_FROM_NOTE = [
    X("06", "resolving tensions #3 (tree vs semi-lattice transclusion) and #4 (surfaces vs components) together"),
    X("06", "completing the math in J (diffs as spans + collapsing towers of migrations)"),
    X("06", "the coherence laws between the 3 algebras + Nix overlays (final vs prev)"),
]

THREADS_02B = [
    {"id": "02b-1", "title": X("02b", "# 1. The box as a stack of machines"), "short": "the box as a stack of machines",
     "work": [("02b", "1. The box as a stack of machines")], "kinds": [OUTSIDE, DERIVED],
     "lines": [X("02b", "So Softland is **shallow in interpretation and deep in composition.**"),
               X("02b", "The closest real machine to this is IBM's System/38, later the AS/400:"),
               X("02b", "Treating all of these as \"compilation\" hides why each one fails differently."),
               X("02b", "If we adopt the VM metaphor naively, it quietly reopens the 6 September ruling. Whether to reopen it is your call; a metaphor shouldn't make it.")]},
    {"id": "02b-2", "title": X("02b", "# 2. The store, derived from scratch"), "short": "the store, derived from scratch",
     "work": [("02b", "2. The store, derived from scratch")], "kinds": [OUTSIDE, DERIVED],
     "lines": [X("02b", "You asked to start from a durable, append-only, ordered log. I agree, with two refinements: the log is atomic **per entry** and ordered **per line**, nothing wider."),
               X("02b", "Many: five clear levels (S0 to S4), one candidate (S5), a sibling (blobs) and an edge (the uncommitted)."),
               X("02b", "The store already wraps like a network stack."),
               X("02b", "Within a layer, the store gives a total order. Across layers, it gives causal order, carried by links.")]},
    {"id": "02b-3", "title": X("02b", "# 3. Fractal layers and planned leaks"), "short": "fractal layers and planned leaks",
     "work": [("02b", "3. Fractal layers and planned leaks")], "kinds": [OUTSIDE, DERIVED],
     "lines": [X("02b", "All of those relations can be made from two primitives:"),
               X("02b", "Your fractal layer and the store's layer can be **the same concept**: reference and overlay, at every scale."),
               X("02b", "Layers are many, fractal and fast. Levels are few, stacked and slow."),
               X("02b", "So a felt promise is often a **bundle** of lower knobs.")]},
]
QUESTIONS_02B_LEAD = X("02b", "Three things are only yours, to carry into the convergence:")
QUESTIONS_02B = [
    X("02b", 'Is "layer" one word for both the store\'s space and your fractal unit? I now think it can be. And what is the word for a level?'),
    X("02b", "Should interpretation depth still stop at one executor (6 September), or does the VM frame pull you toward a language per level?"),
    X("02b", "Which promise words should a typist see?"),
]

# Round 4's four directions (an alternate branch): the many as one building.
DIRECTIONS = [
    {"id": "on", "name": "On and under", "letters": ["A", "B", "C", "D"],
     "lead": X("04", "**On and under: the floors (A, B, C, D).**"),
     "q": X("04", "The question here: how many floors, and what belongs on each?")},
    {"id": "beside", "name": "Beside", "letters": ["E", "H", "I"],
     "lead": X("04", "**Beside: the rooms (E, H, I).**"),
     "q": X("04", "The question here: how do things compose?")},
    {"id": "through", "name": "Through", "letters": ["F", "J"],
     "lead": X("04", "**Through: the shaft (F; who, when, why; authority; J).**"),
     "q": X("04", "The question here: what must every floor carry?")},
    {"id": "time", "name": "Over time", "letters": ["G", "J"],
     "lead": X("04", "**Over time: the renovation (G, J).**"),
     "q": X("04", "The question here: which way does change flow?")},
]
DIRECTIONS_FRACTAL = X("04", "**Your fractal is where two directions meet.** A room turns into a floor when it offers words to the rooms above it.")
DIRECTIONS_ALLONE = X("04", '"All one" is the single store under every floor: one log, one address space. "Modular" is the floors, the rooms and the shaft drawn on top of it.')

# --------------------------------------------------------------------------
# Play: the toys quote these lines.
# --------------------------------------------------------------------------

TOY_FINAL = {
    "nix": X("06", "A Nix overlay is a function `final: prev: { … }`. `prev` is the set as it was before this layer, and `final` is the set after all layers have applied, found as a fixed point."),
    "example": X("06", "In your editor, vim-like has two parts: a keymap, and a status line whose hints are computed from that keymap. Your layer changes one binding and doesn't touch the status line."),
    "extend": X("06", "Your layer holds only `keymap / C-s ↦ save-all`, and the rest of vim's keymap falls through to you. That is Nix's `prev.keymap // { C-s = save-all }`: extending the parent rather than copying it."),
    "replace": X("06", "Per-part delegation would be Nix's whole-value replacement."),
    "final": X("06", "If it reads the keymap through your list of layers, it shows your Ctrl-S. That is `final`, and it is correct."),
    "prev": X("06", "If it reads through vim-like's own layer, it shows stale hints, which is the bug `final` exists to prevent."),
    "rule": X("06", "The one rule to make explicit is that a record must never pin its references to siblings to its own home layer."),
    "cycle": X("06", "Parts computed from each other make the world a fixed point. That fixed point is well defined when no part's value depends on itself. Because records are total, a cycle comes back as a refusal instead of looping forever."),
    "readpath": X("06", "In the read path, the choice between `final` and `prev` happens at the call: `:from-layer` takes the instance's list, while an explicit `:layers` list can name the template's own."),
    "keys_j": X("02b", "Vim's `j` and a plugin's `j` clash only if both are keymap facts."),
    "keys_w": X("02b", "If you pick vim's `w` motion into your layer"),
}
TOY_DIFF = {
    "span": X("06", "A diff between two states is a span `I ← U → J` over what stayed the same."),
    "u": X("06", "If every part has its own number, `U` is given: it is the set of ids present in both states. If identity is a name or a position, `U` has to be inferred, and every diff is a guess."),
    "d3": X("06", "So first-maker decision 3 decides whether level 8 is exact or guessed."),
    "name": X("01", "If a function is its name, that move deletes and remakes about 1,500 things, and every pointer to them breaks."),
    "act": X("06", "Every new fact names the fact it `replaces`, so each act already records its own span: what it removed, what it added, and, by leaving them alone, what it kept."),
}
TOY_MERGE = {
    "pushout": X("05", "Combining parts from two parents is a **pushout** of vocabularies: you glue them along what they share."),
    "sigma": X("05", 'each vim binding gets its own unknown "when"'),
    "pi": X("05", 'all vim bindings share one "when": the default context'),
    "both": X("05", 'Both sets of bindings survive, so where both editors bind Ctrl-S, two binding rows exist side by side. Choosing one of them ("nearest wins") is a separate policy, a query on top.'),
    "self": X("05", "Self, the prototype language, reports an ambiguity error when two parents both answer."),
    "order": X("05", "Python and the store choose by one fixed order."),
    "git": X("05", "Git has the person merging resolve the conflict once, and records the resolution."),
    "knob": X("05", "Error, order or recorded resolution could be a knob on each layer."),
    "data": X("05", "Conflicts are data, not failures, and a later patch resolves them."),
    "sep": X("05", "So the math separates **integration**, which loses nothing and keeps everything, from **resolution**, which is a policy you can change. That matches C: resolution is a view."),
    "vocab": X("05", "P = A +_O B :   O  +  mode  +  when"),
}
TOY_SHARE = {
    "diag": X("06", "Every interface in Poly has a diagonal `p → p × p`. Forward, it copies what is shown to both places. Backward, it merges what comes in from either place into the one part."),
    "two": X("06", "Put the diagonal after the part's single state and you get transclusion: one part, shown in two wholes, editable from both. Put two states side by side (`⊗`) instead and you get a fork: two parts that started equal."),
    "where": X("06", "The algebra tells sharing from copying by where the state lives. In the store, that is two wholes pointing at one entity, versus a new entity that delegates to the old one."),
    "open": X("06", "Merging the inputs makes the override global; keeping them tagged by place makes it apply to that whole only. That choice is a knob, not something the math decides."),
    "palette": X("05", "Your command palette is transcluded from vscode-like's, so the same part sits inside two wholes."),
}
TOY_PACE = {
    "earned": X("05", "So in Softland's fractal, pace emerges from dependents, and it should be visible: how many layers build on this, read it or pin it. That number says how careful a change must be."),
    "edge": X("05", "A layer can be fast for a dependent who follows its latest version and slow for another who pinned it."),
    "absorb": X("05", "*Absorb* (fast into slow): adoption and upstreaming. \"Fast proposes, slow disposes.\""),
    "protect": X("05", "*Protect*, in both directions: pinned versions, so a slow change does not break fast layers that didn't ask for it, and a fast experiment does not reach every live instance."),
    "see": X("05", "*See*: provenance and dependents, so each side knows what it touches."),
    "brand": X("03", 'Brand\'s line is "Fast learns, slow remembers. Fast proposes, slow disposes."'),
    "ed1": X("05", "The act is site and the record language is structure."),
    "ed2": X("05", "The base's keymap vocabulary is skin, and the GPU text renderer is services."),
    "ed3": X("05", "The shared vim-like template is space plan, and your tweaks are stuff."),
}
PACE_ROWS = [
    X("05", "| forever | site | the act's form; the log's promises (order, atomicity, durability) |"),
    X("05", "| decades | structure | what facts, layers and standing mean; the record language |"),
    X("05", "| years | skin, governance | the base vocabulary (what keys mean); the permission model; the base's conventions |"),
    X("05", "| months | services | capabilities; runners; Rama and its deployment; the executor's internals |"),
    X("05", "| weeks | space plan | shared tools that others build on: converters, views, templates |"),
    X("05", "| minutes to days | stuff | personal layers; sketches; candidates being worked on |"),
]

# --------------------------------------------------------------------------
# Rounds: who wrote each prompt (MAP "Whose words are whose").
# --------------------------------------------------------------------------

PROMPT_PROV = {
    "01": [("noprov", X("LOG", 'Sid wrote two notebook pages and opened a session with them: "this is what i have been thinking".'))],
    "02": [("typed87", X("MAP", "After the first reply, typed by Sid (‹87ffd01d›):")),
           ("noprov", None)],
    "02b": [("noprov", None)],
    "03": [("pasted", X("MAP", "the 07:26 rewrite of ‹bb0ad8a0›, which round 3 answered;"))],
    "04": [("noprov", None)],
    "05": [("pasted", X("MAP", "the 11:02 message that asked for round 5;"))],
    "06": [("pasted", X("MAP", 'the "three bridges" paste that asked for round 6.')),
           ("typed9e", X("MAP", "‹9ec182a6› (stop fanning out; converge in a new session)."))],
    # 07: Sid pasted it (the starter prompt a session made from his lines). MAP.md
    # predates round 7 and has no line to quote, so False: the badge, no note.
    "07": [("pasted", False)],
}
LONG_PROMPT_LOG = X("LOG", "I like some of the directions that you took but and seem missing in the output chat")
LONG_PROMPT_PASTE = X("LOG", '[paste of the session\'s text: "Some leaks are honest. A person who is typing …"]')

# --------------------------------------------------------------------------
# Round 13 and the lines around it (13-the-box.md), and the box hub.
# --------------------------------------------------------------------------

BADGES.update({
    "typed55": ("Sid", "typed ‹55a15012›",
                "Typed by Sid on 21 October at 17:31 UTC, seven minutes after round 7's reply, as the record holds it (13-the-box.md)."),
    "typed9a": ("Sid", "typed ‹9ae782df›",
                "Typed by Sid on 21 October at 18:13 UTC, as the record holds it (13-the-box.md)."),
    "typed07": ("Sid", "typed ‹07547fcf›",
                "Typed by Sid on 23 October at 08:24 UTC, as the record holds it (13-the-box.md)."),
    "typedc7": ("Sid", "typed ‹c7c4f903›",
                "Typed by Sid on 23 October at 08:53 UTC, as the record holds it (13-the-box.md)."),
    "typedb3": ("Sid", "typed ‹b32f4df7›",
                "Typed by Sid on 23 October at 09:11 UTC, as the record holds it; the paste between its paragraphs is not his typing (13-the-box.md)."),
})

# His page-2 sentence and the words on the box figure (vision/LOG.md).
PAGE2_SENTENCE = X("LOG", "So what I have currently is a box which can support atomic operations, do some actions and output the effect of those ops to the broader world, you can version your stuff. The starting layer access I have to now is what an ACT is. so now we have a system that gives us input output in form of ACT and that is where we have to meet the user. The question for me now is what can I do and how can I do")
PAGE2_WORDS = {
    "in": X("LOG", "atomic operations"),
    "act": X("LOG", "what an ACT is"),
    "do": X("LOG", "do some actions"),
    "version": X("LOG", "you can version your stuff"),
    "out": X("LOG", "output the effect of those ops to the broader world"),
}

# Sid's lines in 13-the-box.md.
SID13 = {
    "react": X("13", "you mentioned spivak and transformations in database how can that apply to us like i am currently stuck because i can't find the best way to define and find the next layer up like i have the info that we have all gone through in quite a few sessions but i am not feeling it .. it all feels off .. yes there is way to think from the pov of product and what not but i don't think that is the current way i see this its not a beautiful layer that is there, it feels like forcing, the emergence is there i can feel it but i can't grasp it .."),
    "prompt1": X("13", "From the store, no. The full line would be: it keeps who said what, about whom, having seen what, and because of what. Of the four, about whom matters most for people's own data."),
    "prompt2": X("13", "Cool but rama is not a traditional database for storage of data right so there is more how would this combine with that??"),
    "crux": X("13", "what would be better for round 8 to 14 is not verbatim but a crux or a pov to think from like that not verbatim imo"),
    "names": X("13", "the best way that i agree with to talk about this whole is talking in terms of box i think the turn 13 so use that to talk to me"),
    "ask1": X("13", "did you not update it with the convergence or the few parts?"),
    "ask2": X("13", "maybe it means rewriteing and having a better updated presentation of the artifact as a whole or something i think you should ask a fable-max model to do the thinking part of how to present and it can itself use a sonnet-max agent as its worker"),
}

# Round 13's reply («2cb1e5b4»), session text.
R13 = {
    "other_half": X("13", "Yes, and I think Rama is the other half of this picture, not just the place it's kept."),
    "what_rama": X("13", "**What Rama is, in claim terms.** Rama doesn't store data and then answer questions about it. It turns a log of events into derived tables, computed next to the data as each event arrives, and serves them once, on demand, or live. In our words, it's a machine for turning claims into readings:"),
    "today": X("13", "**What the store uses of that today is one reading.** The gates decide each act and keep facts by place, plus facts by value for marked keys. Each fact's row records who wrote it and the permission its act named."),
    "three_log": X("13", "The other three parts of a claim (`based-on`, `because-of`, `:subjects`) exist only in the log. No table holds them, so nothing can read along them without scanning everything ever said. I checked: none of the three appears in the store's table, row or walk code."),
    "directions": X("13", "Each part of a claim is a direction you'd want to read along, and Rama can keep each one live:"),
    "by": [X("13", "**By who:** everything one maker said. That's audit, and when a grant is cut, everything that grant produced."),
           X("13", "**By about whom:** everything about one person. People can see their own data, and forgetting starts here."),
           X("13", "**By what it saw** (`based-on` read backwards): everything said by someone who had seen a given fact. When that fact is replaced, those claims can be marked as made against an older world. That's truth maintenance, done as acts arrive instead of rediscovered later."),
           X("13", "**By what caused it** (`because-of` read backwards): everything one ask led to. That's the trail of the work, and it's how a returning session finds where it was.")],
    "no_mech": X("13", "None of these needs a new kind of mechanism. Each is one more table, kept the way the gates already keep theirs."),
    "both_ends": X("13", "**Rama also offers both ends of every reading.** A reading can be kept as each act arrives: paid once per write, instant to read, live. Or it can be computed when asked, paid on each read."),
    "open": X("13", "**What's genuinely open.** Records are data, but topologies are code."),
    "gap": X("13", "Rama provides the substrate for incremental computation, not an incremental record language. That gap is the hard part."),
}
# The table in round 13's reply: three columns, split each line on runs of two or more spaces.
R13_TABLE = [
    X("13", "a claim                  an append to a depot            (the act, through the door)"),
    X("13", "deciding it              a topology                      (the gate: the store's first reading)"),
    X("13", "a reading kept           a PState a topology maintains   (kept true as each act arrives)"),
    X("13", "a reading on demand      a query topology                (computed when asked)"),
    X("13", "a reading watched        a reactive proxy on a PState    (your watcher)"),
]

# The session's restatement («2b5185b2»), session text; Sid did not answer it.
RESTATE = {
    "into": X("13", "**Into the box: claims.** Every act says who said what, about whom, having seen what, and because of what. The box decides each act whole, accepted or refused, and keeps it forever, in order."),
    "inside": X("13", "**Inside the box: Rama turns claims into readings,** right where the claims are kept. A reading can be:"),
    "inside_list": [X("13", "kept true as new claims arrive,"), X("13", "made when someone asks,"), X("13", "watched live.")],
    "out": X("13", "**Out of the box: readings.** What's true for you is a reading through the layers you chose. Today the box makes only one kind: what stands at each place, nearest layer first, plus lookup by value. Every claim also holds four other directions, but nothing reads along them yet:"),
    "out_list": [X("13", "everything one person said,"), X("13", "everything said about one person,"),
                 X("13", "everything said by someone who had seen this fact,"), X("13", "everything one ask led to.")],
    "threads_h": X("13", "**Our open threads, in the same terms:**"),
    # (where this page holds the matter, the item)
    "threads": [
        ("start", X("13", "**The resident builder** puts claims in and takes readings out. It needs no interface beyond the box's in and out, plus readings that tell it what exists and where it left off.")),
        ("bo-C", X("13", "**Records** (tools, converters, views, checks) are readings written as data. The open question is whether the box can keep any record's reading live without that record turning into code.")),
        (None, X("13", "**Self-building:** Softland's own code goes into the box as claims, and a watcher is a reading. The build itself happens outside, at the box's edge.")),
        ("start", X("13", "**Proofs:** a claim, the theorem, comes with a reading anyone can rerun, the check.")),
        ("tn-1", X("13", "**The act's form:** each slot is a direction you can read the box along, and that's what earns it a place. This has to be decided before the first claim is kept for real.")),
    ],
}

# Round 7 (07-convergence.md), the lines the box hub and the "two few parts" section show.
R7 = {
    "in_atomic": X("07", "**\"a box which can support atomic operations\"**: holds. A gate accepts the whole act or refuses the whole act, with a reason (`act.clj`'s own description, checked)."),
    "in_act": X("07", "**\"The starting layer access I have to now is what an ACT is\"**: holds, with two additions: the answer, and the read. The act is the way in; the read, through your list of layers, is the way out."),
    "in_user": X("07", "**\"that is where we have to meet the user\"**: I'd correct this one. The act is where we meet the **builder**: you writing directly, the agent, or a program."),
    "inside_do": X("07", "**\"do some actions\"**: doesn't hold for the store."),
    "inside_do2": X("07", "The store decides and remembers. Doing belongs to Running (records run by the executor) and to the makers."),
    "inside_version": X("07", "**\"you can version your stuff\"**: half holds."),
    "inside_version2": X("07", "Every act is kept in order, and a fact names what it replaces. That is history."),
    "inside_glass": X("07", "**The box holds for one floor, the store,** as a glass box that remembers and decides. It doesn't \"do actions\". It's where *builders* meet the land; users meet gestures and views."),
    "inside_two_maps": X("07", "**\"The store is five levels fused\" (rounds 2–3) against \"one level, permissions built into every row\" (7846018a).** Both are right, on two different maps."),
    "out_effect": X("07", "**\"output the effect of those ops to the broader world\"**: holds if it means anyone allowed can read what changed, once or live. It doesn't hold as acting on the world: nothing in the store reaches out."),
    "out_read": X("07", "You read through a list of layers, nearest first, once or live."),
    "out_notknow": X("07", "The face also lists what the store must not know:"),
    "out_notknow_list": [X("07", "what a value means;"), X("07", "what a thing is beyond its id;"), X("07", "names;"),
                         X("07", "that anything depends on anything;"), X("07", "how to run anything;"),
                         X("07", "how anyone's identity was established.")],
    "builder": X("07", "**\"next up is writing into it and having an agent interface how? … the resident builder\"**: the agent isn't a floor and needs no interface of its own."),
    "builder2": X("07", "It's a maker, outside every floor, using each floor's face."),
    "floors": X("07", "**The floors:** Store (built), Meaning, Running, Making and Surfaces, over Rama. The waist runs down the middle of every floor."),
    "rows": X("07", "**Rows are floors: what runs on what.** A floor exists where there's an engine: code that offers a small language to the material above it."),
    "cols": X("07", "**Columns are who may change a thing, slowest to fastest.** Our code in git, then the base, then shared layers, then yours."),
}

# The box hub: what each of the earlier rounds said about each face.
HUB = {
    "in": {
        "r2": [X("02", "**What holds.** Seen from above, a level should be a box. You know what goes in, what comes out, and what it promises. You never need the inside."),
               X("02", "So ask one question of each slot: does the store check it, or only carry it? What the store checks belongs in the form. What it only carries could be content instead: facts inside the act, owned by the level that needs them.")],
        # (breakout or thread to go to, its line, a second line or None)
        "many": [("D", X("03", "**The act is a waist, and it is getting fat.**"),
                  X("03", "The store can check who and permission completely, so those may belong in the waist. A label that no gate reads gains nothing from being in the middle.")),
                 ("K", X("05", "**An act names the permission it acts under.** The gate checks that permission. It does not look up everything that `who` may do."), None),
                 ("02b-2", X("02b", "Every part of the act belongs to some level:"), X("02b", "The store already wraps like a network stack.")),
                 ("F", X("03", "**Name each knob where the person who cares about it lives.** \"Decided at once\" is the typing person's word, and the store translates it into its mechanism."), None)],
    },
    "inside": {
        "r2": [X("02", "**It is not one box.** The store as built is two levels fused together (my reading of the code):"),
               X("02", "**Its wall leaks.** Every act-id is `[layer depot uuid]`, and the depot is `:streaming` or `:microbatch`, Rama's two kinds of topology (checked, `act.clj`)."),
               X("02", "**It must open.**"),
               X("02", "**Hide Rama's words, not its strengths.**"),
               X("02", "An interface that any database could fill would throw away the reasons Rama was chosen: computation next to the data, live reads, and a store that comes out the same when it is run again. Keep those, and state them as promises in Softland words.")],
        "many": [("B", X("03", "**The store as built is five of these levels fused:** log, facts, live, layers and standing (my reading of the code)."), None),
                 ("C", X("03", "The log is truth. The current state is a view. Layer resolution (nearest wins) is a view."),
                  X("03", "If layer resolution is a view, a person could one day define a new kind of layer as a record (\"the majority of these three layers wins\") without changing the store's code. The waist would move down one level.")),
                 ("A", X("03", "Softland's instruction set is the act and the read. Every maker writes in it, and Rama implements it the way microcode implements an instruction set."), None),
                 ("02b-2", X("02b", "Many: five clear levels (S0 to S4), one candidate (S5), a sibling (blobs) and an edge (the uncommitted)."), None)],
    },
    "out": {
        "r2": [X("02", "**What are we building on?** A shared store of facts. Every write is an act by someone, in a layer, under a permission. Every read goes through a list of layers, nearest first. Rama was the floor for building that. From here up, nobody should need a Rama word."),
               X("02", "The levels are levels of *reading*, not of storage: each level's engine reads the same store in its own words.")],
        "many": [("H", X("05", "**The store's reads are Plan 9 unions.** A read through a list of layers, nearest first, is a union directory searched in order, and \"a view may give another list\" is a per-process namespace."),
                  X("05", "**\"Where did this come from?\" needs its own answer.**")),
                 ("E", X("03", "**A and E fit together.** The computer's fixed levels are the floor, and the fractal grows on top of them. The waist is where one turns into the other. Below it there are a few fixed levels in code. Above it there are layers on layers without end, all of them records."), None),
                 ("J", X("05", "**Δ is a view.** It reads D's data in C's words: for each kind in C, look at the kind in D that it maps to."),
                  X("05", "**flattening typed data into facts loses nothing if, and only if, you keep the vocabulary and each thing's kind.**")),
                 ("C", X("03", "the document is a view of the log, undo is the log read backwards, and \"my view of the document\" is a view with my overrides."), None)],
    },
}
