# Decision 4: who acts, and in which session — a reviewer's own thinking

Phase one of a two-phase review, written before reading the page that another
agent is writing on decision 4. Reviewer: Opus 5.5 at max effort, fresh,
started by the orchestrator. Repository at commit c90c7bcc, night of 14 to
15 October 2026. Nothing here is decided. A default is marked as a default,
with its reasons; the decision is Sid's.

How claims are marked. `path:line` is a read of the source at c90c7bcc. "Run N"
is one of my evaluations with `clojure -M:store -e` (pure code), listed in 2.10.
‹id› is a line Sid typed; every run of his words quoted here was put to
`memtool who` and came back "typed by Sid" unless the text says otherwise. «id»
is a session's text, never his words. [mine] is my reasoning. [not checked] is
what I could not check.

## 1. The question, cut again

**1.1 Four questions travel as one.** "The system in the base, or my own layer
building on the base" fixes three things at once: whose permission is at the top
of the chain an act stands under (the authority), what the act's `:who` says
(the actor), and which layer takes the facts (the place). "What an ingester's
session is" is a fourth: the run. "Who may act as the system" is a fifth, about
the sender and not the act. The road's last leg, "back out through the read
path", has a sixth: who asks. [mine] The two named ends are two corners of a
larger space, and the ends that serve the road best are mixed ones: the system's
chain with a named actor under it; the base for some paths of the repository and
a layer above it for the rest. Section 4 lays the space out.

**1.2 The system is not Sid.** The store's word for it: "software that acts for
the architects of the medium" (`src-store/softland/store/gate/first_act.clj:44-49`).
Sid's: "the system basically software", and "if it is a system level thing like
we as the architect of it are trying to do then it comes under that part"
(‹ddf13d17›, 6 October). The composing session's option A, "the runtime starts as
you, the system holder" («f9be15b2»), merges the two. In the store the base's
holder is the id `:system`, and any program that reaches the door can send acts
under it (2.4). If Sid's work, an agent's work and the ingester's work are all
sent as `:system`, the record says "the system" of each of them for good (2.2).
[checked; the last sentence mine]

**1.3 The ingester as a thing, and a run of it.** On 13 October Sid put ingesters
among what the base is: "any user who gets onboarded to Softland, they have this
uh, basic admin provided things that they can do. Maybe like ingestors or some
other stuff" (‹4123e23c›, dictated). On 6 October he put an ingest act under the
one who runs it: "by default we will say if a user is trying to ingest something
it is by user's permission under user's permission" (‹ddf13d17›). The two lines
agree once the ingester as a thing (its code, or its record if decision 1 makes
it data) is kept apart from a run of it: the thing can be the system's, provided
in the base, while a run acts under whoever runs it. [mine] An ingester built to
act as the system because the system provides it folds the two together, and the
first person who ingests her own notes meets that fold.

**1.4 Two concerns meet in this one road.** The vision log holds, from the June
notebook, "There are 2 diff problems. code ingestor": a person onboarding an
existing codebase ("Import → work with llm on code → check what maps to what and
then accept or reject code changes"), and "another side to this i.e Softland
code", Softland as "a truly distributed runtime that ships with default
capabilities but ... users (AI, humans) can expand it" (`vision/LOG.md:226-241`;
`docs/carry-on.md:101` gives the two concerns). [checked. The record holds the
first of those runs only in a session's text, «6c79c222», so the passage may be a
session's transcription of the notebook; not checked whose wording it is.] This
repository is both at once: a codebase being onboarded, and Softland's own code.
Decision 4 chooses which of the two the first road is an instance of. "The system
in the base" makes it the second, a case with one inhabitant. "My own layer" makes
it the first, the case of every other inhabitant: "a new user can come and
describe the adapter for whatever external data that they have" (‹6b40e2e2›,
2 October). [mine]

**1.5 A divergence that sits inside this decision.** Sid typed, to two sessions,
at 17:18 UTC on 14 October: "but to do that we need the sign-in, inlets, adapter,
converter atlest for the code and md part" (‹bec8ca63›, ‹09aa5d93›). One session
answered "One correction, though: the code and markdown road doesn't need sign-in
yet" («ca81bcf6»); the other wrote "I said 'no sign-in' too flatly" and put A (the
runtime as the system) and B (Google sign-in) to him («f9be15b2»). He answered
neither; his next line was about the night's sessions (‹0629ab5e›). The pasted
prompt then says "this road needs no sign-in yet": a session's sentence
(«3c021c3a»), which he pasted and did not type. That sentence assumes the end
that decision 4 is to choose. Its reason, "the store already knows system and
base", is narrower than it reads: the gate takes a person's UUID making a layer
that she holds as readily as it takes the system making the base (runs 1 and 4).
What sign-in adds is not the gate's acceptance. It is a person id that is the
store's own and that her later sign-ins join (9 October entry 1,
`src-store/README.md:355-369`; K7, K8). So "no sign-in" holds for the system end,
and for the own-layer end only if a person id is made by hand (4.2, W3). Each end
forces one open question: the system end needs somebody to send acts as `system`
(K9, carried for now by the operator ruling, 2.4); the own-layer end needs a
person before sign-in, and no act as the system at all beyond making the base.
[checked; the inference mine]

**1.6 The lens's provenance.** The cost lens was first written by a session in
«3c021c3a» (17:31 UTC, 14 October) and carried into «cca75ac4» and «cecfdf3c»;
`memtool who` gives it as session text that Sid pasted. Its anchor, "my decision
6 of 13 October", is his: the prompt of the build "run again", which he sent
(`src-store/README.md:941-951`).

## 2. What the store does today

**2.1 The four parts.** `:who`, `:layer`, `:permission` and `:session` are parts
of every act (`src-store/softland/store/act.clj:152-161`). `:session` must be
there (`:no-session`) and is an id, a readable keyword or a UUID (`act.clj:158`,
`act.clj:343`); Sid decided it required (6 October entry 4, `src-store/README.md`,
"Decisions of 6 October 2026"). No namespace under `gate/`, `module/`, `record/`
or `read_path/` reads `:session` (grep at c90c7bcc). "It has no rule about a
session beyond its shape" (`act.clj:563`); the door carries it as given
(`door.clj:72-75`). The act's namespace docstring gives "the rules of a session"
to the gates (`act.clj:77-78`); the gates have none, so that line names room and
not a rule.

**2.2 Where each part stays.** The depot keeps the whole act as sent, for good
(`src-store/softland/store/door.clj:77-83`; "forever never trim", ‹727f622d›,
`src-store/README.md:746-749`). A fact's row keeps `:who` and `:permission` as
text, and not `:session` (`src-store/softland/store/record/rows.clj:24-31`). The
permission's place is keyed by its holder, "<layer> <holder>
:softland.store/permission" (`rows.clj:45-46`; `gate/decision.clj:111-119`). The
layer is the first slot of every act-id, so of every fact-id (`act.clj:175-180`),
and the first word of every row key (`rows.clj:163`, `rows.clj:173`). [checked]
So the session lives only in the log; `:who` and the permission live in the log
and in the facts table; the layer lives in both, and in every reference that any
other act makes to these facts through `:based-on`, `:replaces` or `:because-of`.
[mine, from those lines]

**2.3 Who may make what.** A making act names a first permission `[holder layer
layer]`; its `:who` is the holder or `system`; the base's holder must be `system`;
it holds exactly two facts (`first_act.clj:81-122`). The system can hold any layer
(9 October entry 3, `src-store/README.md:400-421`; `first_act.clj:24-28`). A
person can make a layer that she holds (run 4): the road by which "the person who
creates the group makes the group's layer" (8 October entry 2,
`src-store/README.md:299-311`). A second making act for a layer that is made is
refused (`decision.clj:154-205`; run 13 gives the word for the same rows).

**2.4 Nobody's identity is checked, for now by design.** "Until sign-in is built,
any sender can make any layer that is not made, for any holder"
(`first_act.clj:30-33`). "`:system` is treated as any who" (`door.clj:109-111`);
"who asks is what the caller says, and `:system` is a who like any other"
(`read_path.clj:262-263`). What bounds it: only programs on machines that Sid runs
may reach Rama's ports (12 October, W9 A, `src-store/README.md:557-567`); and the
September ruling "Operator trusted at launch. No signing; no slot needed, a later
edition can add the part. Facts from before that day rest on the operator's word"
(`src/proposal/frame-2026-09-15/PROGRESS.md:63-64`). Of that ruling, Sid's words
are only "1. spoken in the door will be left open but yeah I will uh, by default
have A" (‹1088472f›), his answer to «ca29636f», which had told him "Permissions
written before signing starts will rest on your word for ever" and offered "(a)
for now, with the door left open". The ruling's last sentence is the session's
(the rulings file's own table, row 63–64). "Spoken" reads as dictation of
"signing" [not checked].

**2.5 An act stands only under its own `:who`'s permission.** A permission fact
must sit on the holder that its first slot names (the rule
`:permission-fact-sits-on-its-holder`,
`src-store/softland/store/door/declaration.clj:549-553`), and a gate reads the
permission at the place of the act's `:who` (`decision.clj:61-68`,
`decision.clj:154-205`). So in every act but a making act, the actor and the
holder are one id. Runs 13 and 14: once the system has made a person's layer, it
cannot act there (`:who-not-the-permissions-holder`; or
`:permission-does-not-stand` when it names a permission of its own). The queue's
V13 says the same, as the builder's reading. The rig let a session hold a
permission beneath its person's (`src/proposal/rig-2026-09-25/src/rig/store/permit.clj`,
docstring of `refusal`, "a session may hold a permission beneath its person's");
the store does not: here only `:who` can hold. (Example; evidence only.)

**2.6 One permission for each holder in a layer; none ends; "see" stays "see".**
`declaration.clj:224-230`; `decision.clj:61-68` ("nothing in this build ends a
permission"); `decision.clj:302-330`; `src-store/README.md:503-527`.

**2.7 Whoever may act may give, and may change the settings.** A holder of "see
and act" can give a permission straight beneath her own (check (g),
`decision.clj:267-281`; E2 A, a session's reading of his answer 4,
`src-store/README.md:312-328`), and can write the layer's settings, who may see it
among them: "any such holder in the base can set the base to `:members`"
(`decision.clj:283-300`). A permission says `:see` or `:see-and-act` and nothing
else (`act.clj:187-190`): no kind may act and may not give.

**2.8 The base and every other layer.** The base is microbatch
(`first_act.clj:55-60`), one line decided on one task, with its rows copied on
every task (13 October entries 3 and 4, `src-store/README.md:903-922`), and it
starts as "everyone", kept only so that the build can run
(`first_act.clj:139-146`; V25). Every other layer is streaming, keeps its rows on
its own task, starts as "members", and builds on nothing until a fact says so
(`first_act.clj:128-133`). A read of the base that starts from another layer reads
the base's copy on that layer's task, and can show it a batch behind
(`src-store/softland/store/read_path/README.md:191-213`). A read lets the nearest
layer that holds a fact at a place win (11 and 12 October entry 2,
`src-store/README.md:528-541`).

**2.9 What the proofs play.** Every act of the door's proof names `:session
:door-proof`, one constant for every actor
(`test-store/softland/store/door_proof.clj:502`, `512`, `533`, `538`); the system
makes each person's layer (`door_proof.clj:498-508`). The module proof's line
makes the base, gives Alice a permission beneath the system's, and has her act
under it; its check says that the three are accepted in order
(`test-store/softland/store/module_proof.clj:2118-2155`; I did not run it).

**2.10 My runs** (`clojure -M:store -e`, 14 October; a UUID stands for a person,
another for an ingester, a third for a run's session):
1. the base made by `system`, for the microbatch gate: a making act (true);
2. a streaming layer `:softland-code`, made and held by `system`: true;
3. the base with a person as its holder: false;
4. a layer made by a person (a UUID) who holds it: true;
5. a layer made by `system` for a person who holds it: true;
6. `system` giving an ingester `[ingester :base :base [:system :base :base]]` in
   the base: `act/parse` gives `:ok`;
7. the ingester's act under that permission: `:ok`;
8. `:session` as the string "0e6182b9": `:malformed`;
9. `(keyword "0e6182b9")` is not a readable keyword;
10. `:session :run-0e6182b9`: `:ok`;
11. `:who` as a string: `:malformed`;
12. no `:session`: `:no-session`;
13. `standing`, `system` acting in a person's layer that is made, naming her
    permission: `:who-not-the-permissions-holder`;
14. the same, naming a permission of its own: `:permission-does-not-stand`;
15. `:session (keyword "0e6182b9")`: `:malformed`;
16. `:session (keyword "c90c7bcc")`: `:ok`;
17. `:session`, a name-based UUID of the commit: `:ok`.

And from git at c90c7bcc: 1,136 of the repository's 1,858 commit hashes begin
with a digit; 326 `.clj` and 1,772 `.md` files are tracked; 149 commits since
1 October, 148 with the author `sid597` and one with "Siddharth Yadav" at the same
address.

## 3. What Sid has said, and what only sessions said

His lines (typed unless marked):

- ‹ddf13d17› (6 October): three that act. "some user is taking an action"; "the
  system basically software"; and a group: "so a group can have its own agents
  that are defined we have their keys etc etc so someone is using that then it
  means like it is part of the group". The default for an ingest is the user's
  permission when a user ingests; system-level work "comes under that part". He
  speaks of a permission, not of a session.
- ‹00121720› (6 October): "yess", to "an agent's credentials for a model
  provider": where E7 comes from.
- ‹d05a12e6› and ‹f3f530b8› (8 October by his clock): "1. system"; a signed-in
  user "automatically" has a personal layer and permissions; an unsigned user is
  "just roaming around on what is open by the system or world group"; to E3,
  "what and why is this question??? no me understand"; and "permisison is like a
  hierarchy ... it ties back to a single individual even if its invoked in a
  group->group->agents->agents-groups-Agent group->... backtracking will come
  down to who created the grouph, invited who, who gave the permission".
  ‹751c2af7›: "2. ok", to a session's form: the system makes the personal layer
  and the permission at the first sign-in, and the creator of a group makes its
  layer.
- ‹6d5e8c3c› (9 October; a long prompt that the record cannot tell from a paste;
  it is first in the record there and reads as dictation): "my preference is a
  uh, system can have uh, many layers"; the system's layers "kind of libraries";
  "converters or importers, ingesters"; "any other person can make their own";
  the Linux line between what is shipped and what is not. The decision itself is
  ‹15af6243›: "4B and all else A".
- ‹56a55527› (2 October): "world is the open group that every group if make open
  their data publishes to". ‹12fed86b› (2 October): "working on a shared
  canvas/roam research graph with my 10 agents ... very very little personal here
  the whole graph is shared to everyone in the group ... But the "own work feels
  instant" is still needed".
- ‹ac4db2eb› (2 October): "we will use google login) and then based on that we
  start the session with what happens when a user logins".
- ‹1088472f› (23 September): no signing at launch, the door left open for it
  (2.4).
- ‹a06ac414› and ‹4123e23c› (13 October, dictated): the base "from a product point
  of view" as "maybe the global feed that gets deployed on Mastodon. So that
  anyone can go there and just surf around"; then "maybe I conflated many things.
  The base there means like the code that we are writing, anything thad can be
  written in code. And then every other person can use to build on top by having
  data as code"; ingestors among the "basic admin provided things that they can
  do"; "at that layer things are only going to change by either me making the
  changes or a few set of people or agents making the changes to the core of uh,
  system. And no one else would have access to it. And it would be a thing that
  will affect everyone in Softland world." ‹1bd5708f›: "the code that we are
  writing and maybe a few data based layers or something". ‹257c45e5›, to "base
  — the one layer everyone can see": "what exactly is that you just put it out
  like a gospel of truth".
- ‹86db6d30› (6 June): "Once we have ingester the ingested data will be in
  softland format. So they would be the base layer and then we would be using
  them in some way as active layer" (`vision/LOG.md:118`). The log goes on: "say i
  have some code i want to modify the last commit is its base layer whatever i
  build on top is in the active layer ... like prototyping, researching,
  exploration vs bedrock" (`vision/LOG.md:146`; the record holds this run only in
  a long prompt of his, ‹5940b98d›, and in pastes: the log's words, not checked
  as typed).
- ‹41acf242› (1 May): layers for "the code files", for compute, for notes "like a
  layer that takes the above 2 layers as base and can talk about them", and the
  coding agents as "the meta meta layer that can take all 3 above reference them
  and then also crud them".
- ‹6b40e2e2› (2 October): "a new user can come and describe the adapter for
  whatever external data that they have".
- ‹60c1bcaa› (10 August): "i made the repository closed source".
- ‹5ff89890› (12 October): a default takes "the side of spectrum that captures
  more because we are not choosing the destructive side by defualt that can
  happen even after but the other side cannot".
- ‹bec8ca63› (14 October): "we need the sign-in, inlets, adapter, converter".

Only sessions said these:

- That the top of a chain is "a person, or the system for the base, and never a
  session" (E3 B), read onto his answer 4 by «c738ecc3»; E3 stays open
  (`src/proposal/backend-on-rama-2026-10-02/decisions/OPEN-QUESTIONS.md:100`).
- "The session is the root; narrower permissions are ordinary facts under it"
  (rulings file, `frame-2026-09-15/PROGRESS.md:56-57`, «a3a890dd»; "his words in
  part"); "Layer kinds, four ... session split into hand and agent by the
  session-start fact" (`PROGRESS.md:109-112`; "a session's, written on his go").
- E6 on the printed sheet: "You leaned today: the person's when a person ingests;
  the system's for system work"
  (`/mnt/data/projects/research/softland/all-decisions-rest-2026-10-06/make_rest.py:48-50`).
  His line ‹ddf13d17› says "permission"; "session" is the sheet's paraphrase.
  «b9bbf124» held it as "a leaning".
- "Under A more chains end at the system, which is nobody" («8d75dab2»).
- The first maker as "a converter ... It acts as the system, which forces K9"
  («181897ea», 9 October). Sid answered with a question about coupling
  (‹d74ace1a›), not about the actor.
- "the system, in the base, with one session per run named after the commit it
  read" («ca81bcf6») and "The ingester as the system in the base, one session per
  run; your notes in your own layer later" («e73248af»). Two sessions of
  14 October expect the page to land there.

## 4. The ends

### 4.1 Where the repository's material goes

- **P1. All of it in the base.** Gives: the one place every reader reads, on her
  own task (2.8); no person and no new layer; fits "the code that we are writing"
  (‹4123e23c›). Costs: the base starts as "everyone" (2.8) and the repository is
  closed (‹60c1bcaa›), so the day the read path serves anyone outside, all of it
  is open to them unless the base's setting is decided first (V25, K14). The
  base then mixes Softland's core with proposals, history, research, memory files
  and the vision log (1,772 tracked `.md` files beside 326 `.clj`), which is more
  than what he said is "only going to change by either me ... or a few set of
  people or agents" and "will affect everyone". Every fact is written on every
  task (2.8), and the base becomes one microbatch line with every writer in it.
  Two ways to keep a base closed without a new rule are there [mine]: a fact
  that sets it to "members" after its making, with "see" given to each person at
  her first sign-in (one more row for each person, on every task); or a third
  word, "signed in", for who may see (a new value of a store key: a new version
  of the act).
- **P2. A layer that the system holds, streaming, beside the base.** Gives: the
  system's (9 October entry 3), answered at once, one task, starts "members" so a
  closed repository stays closed. Costs: whoever reads it reads it on its one task,
  the very load that the base's copy was made to spread (13 October entry 4); a
  reader sees it only if her layer builds on it, or the base does (a fact either
  way).
- **P3. Sid's own layer, building on the base.** Gives: the person's road (1.4);
  streaming; "members"; attributable to him. Costs: a person id before sign-in
  (4.2, W3); K10's care for the layer's id (no `:sid-notes`); if his personal
  layer is made now by hand, the sign-in that comes later must take it over
  rather than make it again, because a second making act is refused (2.3).
  Softland's own code then lives in a person's layer, and the Linux line between
  what is shipped and what is not (‹6d5e8c3c›) is not drawn by the record.
- **P4. A project layer that Sid makes as its creator, with agents given
  permissions under his.** Gives: his picture of the work, "my 10 agents" and
  "very very little personal here" (‹12fed86b›), and his group with its own agents
  (‹ddf13d17›); many writers, each named; streaming. Costs: as P3 for the person
  id; membership is "whoever holds a permission" (11 and 12 October entry 1, a
  session's reading).
- **P5. Split by commit: the last commit in the base (or in P2), the work on top
  in a layer that builds on it.** This is the log's "the last commit is its base
  layer whatever i build on top is in the active layer" (`vision/LOG.md:146`) and
  his June line on ingested data as base and active layer (‹86db6d30›). The read
  path already lets the nearest layer win (2.8), so a view from the upper layer is
  "the commit, as my work changes it" with no new code. Costs: two layers and two
  kinds of writer from the start; how a change moves down (a commit made outside,
  then read in again; or a second act into the base) is open (?10,
  `OPEN-QUESTIONS.md:94`); entity ids must be the same in both layers
  (decision 3).
- **P6. Split by path.** The core (by his 13 October line, `src-store/` and its
  page of decisions, the walk's two files among them) in the base; the rest
  (proposals, history, research, the vision log, memory) not in the base: in P3
  or P4, or not read in yet. Costs: a map from path to layer, itself code or data
  (decision 1); and where the core ends, which is his to draw.

### 4.2 Who the act says acted, and under which chain

- **W1. `:system`, under `[:system :base :base]`.** No new id; what the proofs
  play. Every act of the road says "the system": the facts table cannot tell
  Sid from an agent from the ingester from a later timer. Who changes the base is
  then the whole of K9. With S1 and S7, facts about each session could say later
  who ran it, but a read of a fact would still give "system" as its writer
  (2.2), and the link would be found only through the log or a later table.
- **W2. An ingester id, under a permission that the system gives beneath its
  own.** One give at the start (run 6), then each act names `[ingester :base :base
  [:system :base :base]]` (run 7); the give and the first act can go in one line
  with no wait (2.9). The top is still the system: "comes under that part"
  (‹ddf13d17›). A read of a fact gives the ingester as its writer (W12 B,
  `src-store/README.md:603-614`); a second ingester (chats, papers) is told apart;
  once a permission can end (V10), one ingester can be stopped without touching
  the system's. Costs: an id kept in the runner's settings; a chain of two in
  every act; and the ingester, holding "see and act", can give beneath its own and
  can change the base's settings (2.7).
- **W3. Sid's own id, a UUID.** Under the system's in the base (`[sid :base :base
  system-own]`), it answers "me making the changes" (‹4123e23c›) and "it ties back
  to a single individual" (‹d05a12e6›); at the top of his own layer, it is P3 or
  P4. Costs: "No code makes a person yet" (`src-store/README.md:368`). A UUID made
  by hand must be the person whom his first sign-in later joins. The queue's
  working text is that a sign-in with no link "makes a new person", and "Two
  persons cannot be joined by writing acts again" (K8, `OPEN-QUESTIONS.md:114`).
  So W3 on the first real record needs K8's half "an account joined to a person
  who exists" first; that half is needed anyway, for his own case of a second
  account (9 October entry 1).
- **W4. The agent that runs the road (a Claude or Codex session), under Sid's
  permission.** The truest to how the work is done now: agents make most commits
  under `sid597` (2.10). It is the E4 reading, "each actor acts under its own
  permission" (`src-store/README.md:317-318`, a session's). Costs: what an agent's
  id is, and whether its credentials say whose act it is (E7, open); and Z4 B,
  "a model's [reads], always" (`src-store/README.md:752-764`), reaches it once
  reads are kept, while the store cannot yet tell a model from a person (same
  entry).

What git can and cannot say [mine]: every commit since October has the author
`sid597`, whoever wrote the change. The act's `:who` is whoever brings the
material in, not whoever wrote it; who wrote a change is a fact about the source.
The old server joined Claude Code transcripts to the commits that they name
(`src/app/server/ingest/git_import.clj:440-470`; an example, evidence only), so
that link is there to be had as content.

### 4.3 What an ingester's session is

The store asks only that there be one, an id (2.1). What a session means is open
in five places: E3 (the top of a chain), E5 (acting after it closes), E6 (an
ingester's), W12 box C (a fact's session within reach, `src-store/README.md:603-614`),
and his own picture of a session that starts at login (‹ac4db2eb›). The ends:

- **S1. A fresh random id for each run.** It says nothing, the reason of
  9 October entry 1 for a person's id, carried to sessions by a session's rule
  (K10). It is the finest grouping that the log can give later: rows do not keep
  the session (2.2), so any table of acts by session is made by a second run over
  the depots, and is no finer than the values written now.
- **S2. One for the life of a process** ("holds one session for the run",
  «f9be15b2»). Coarser: runs in one process are not told apart.
- **S3. The session of whoever started it.** A person's sign-in sitting (his
  picture, ‹ac4db2eb›); for an agent, its conversation. The truest link, and not
  there before sign-in. For an agent, a conversation's id is another system's id;
  the store's own random id with a fact that links the two keeps the store's ids
  its own. [mine]
- **S4. Named after the commit it read («ca81bcf6»).** It mixes what was read into
  who ran: two runs over one commit share it, and a run over a range has no one
  name. Made straight into a keyword it is refused for most commits (runs 9 and
  15; 1,136 of 1,858 hashes here begin with a digit); a name-based UUID passes
  (run 17) and keeps the mix.
- **S5. One constant for every run.** What the proofs do (`:door-proof`, 2.9) and
  what the old server did ("durable-ground-cluster",
  `src/app/server/door/cluster.clj:257-261`; an example, which also kept its
  cursor in a file beside the store that a rebuild must be matched to). The
  grouping by run is lost for good, because the log keeps what was written
  (‹727f622d›).
- **S6. A session as a layer.** The September design's session layer, hand or
  agent (a session's, `frame-2026-09-15/PROGRESS.md:109-112`), with a promotion
  into the target: a run's acts kept apart until they are accepted, as a branch
  is. Not the shape for a mirror of git; the shape for an agent that proposes a
  change ("accept or reject code changes", `vision/LOG.md:228`). Costs: a layer
  for each run, and a second act for each promotion, made by a maker, since no
  topology appends (?10).
- **S7. A session as an entity that facts describe** (who started it, which
  inlet, which commits, whether it is closed). S1 or S3 with facts beside it; it
  lets E5 be answered later without a new part of the act. Its keys are a tool's
  keys, so their form waits on K5.
- **S0. No session for a program.** A change of the act's form, against his
  decision of 6 October (entry 4): a new version of the act.

If a session is ever to hold a permission (E3 A, "the session is the root"; or
the rig's half of E4 A), the gate's rule that only `:who` holds (2.5) changes,
and that is a new version of the act. [mine]

### 4.4 Who may act as the system (K9)

- **K9-a. Whoever reaches the door** (as built, 2.4), bounded by W9 A and resting
  on the operator's word (‹1088472f›). Enough while only Sid's machines can send.
- **K9-b. A check in front of the door**: sign-in for programs, a credential that
  a runtime holds for `system`. The door's own text: sign-in "would refuse a sender
  before this code runs, and changes none of it" (`door.clj:109-110`). Acts sent
  before it stay on the operator's word.
- **K9-c. A part of the act** (a signature), or a rule of a gate for acts of
  `system`. What the session's half of the September ruling foresaw: "a later
  edition can add the part" (`frame-2026-09-15/PROGRESS.md:63-64`).
- **K9-d. By shape: the system acts only where nothing else can.** It makes the
  base, gives the first permissions in it, and makes personal layers at sign-in;
  architects, agents and ingesters act under permissions that it gives (W2, W3,
  W4). K9 then shrinks from "which people may pass as the system" to "which code
  runs as the system". His 13 October "me ... or a few set of people or agents"
  reads either way: a few who may pass as the system, or a few who hold
  permissions under it. [mine] If the base is used at all, someone sends its
  making act as `system` once (`first_act.clj:118`).

### 4.5 Who reads back

The road's last leg asks as someone (`read_path.clj:262-263`). The base starts
"everyone", so any asker reads it; a "members" layer (P2 to P5) needs an asker who
holds a permission there (2.8). The answer that costs least is that a run reads
back as the actor that wrote. [mine]

## 5. The lens, applied, and where it does not fit

The lens (a session's, 1.6): a choice in a depot record keeps its rule in the code
for ever as a version of the act; a choice in a table only is made again by
running the depots again, with downtime; a choice in the read path only costs
nothing. Where each choice falls, with the reason beside it:

| Choice | Lens | Why |
|---|---|---|
| P1 to P6: which layer | none of the three | a value in every act-id, fact-id and row key; no rule changes |
| the base's "everyone" at the start | read path | `first_act.clj:134-146`: no gate calls it |
| a third word, "signed in", for who may see | depot record | a new value that a gate must accept |
| W1 to W4: which `:who` and which chain | none of the three | a value in the log, in each row's text, in the permission's place key |
| a give to an ingester (W2) | none of the three | one more act; no rule |
| ending a permission (V10); a kind that may act and may not give | depot record | a rule of a gate; a new value of the permission's last slot |
| S1 to S5, S7: which session value | none of the three | a value that the log alone keeps |
| a table of acts by session, later | table | made by a second run over the depots |
| a rule on sessions, later (E5, E3 A) | depot record | a change in what a gate accepts |
| S0: no session | depot record | the act's form |
| K9-a | none | nothing is written |
| K9-b | none of the three | in front of the door |
| K9-c | depot record | a part, or a rule of a gate |
| K9-d | none of the three | values and gives |

Where the lens holds: for forms and rules. Ending a permission, a kind that may
act and not give, a signature, a rule on sessions, and dropping `:session` are each
a change in what a gate accepts, and each is a version of the act from the first
real record on (13 October entry 6). A table of acts by session is the lens's
second cost exactly: the depot holds every session value, so a second run can
build it.

Where it does not fit, three places [mine]:

1. **A value in the record.** Most of decision 4 chooses values, not rules: which
   layer, which `:who`, which chain, which session. No rule changes, so the first
   cost does not apply; and they are not in a table only or in the read path only.
   Their cost is a fourth kind. Before the first real record, nothing. After it,
   the value stays in the log for good (‹727f622d›) and a second run gives it back
   as it was. To change it is to write new acts (a copy into another layer,
   retracts in the old one), and every act of another layer that names those facts
   (a `:based-on`, a `:replaces`) still names the old ones, with the old layer in
   each id (2.2). So the cost grows with what is built on the material, not with
   any rule. The lens as written scores these choices "costs nothing" by leaving
   them out.
2. **In front of the door.** Sign-in and any credential for `system` sit before
   the depot. They change no record and no table, and the read path does not see
   them. Their cost falls on the past: each act sent before them rests on the
   operator's word for good.
3. **"Costs nothing" in the read path is a cost of engineering, not of meaning.**
   The base's "everyone" is read-path code; changing it moves no data and changes
   what every layer with no fact shows to every asker, at once
   (`first_act.clj:142-143`). For a closed repository written into the base, that
   one line is the difference between private and public.

The lens says nothing of time. All its costs start at the first real record
(13 October entry 6: "This starts with the first real record"). Whether the first
road writes the first real record is open (V23, `OPEN-QUESTIONS.md:189`); by the
page of what must be true first, real data waits for the day a real cluster
starts (`src/proposal/backend-on-rama-2026-10-02/WHAT-MUST-BE-TRUE.md`). So for
the road itself every end of decision 4 is free in the store; what is not free is
what the road's code assumes, because the same code writes the first real record.

## 6. Consequences

**6.1 For the first road.** On a store that is not kept, any end costs nothing in
the store. What carries forward is the shape of the code. If the converter holds
`:system` and `:base`, the first real record gets them. If a run takes its actor,
its permission, its layer and its session as inputs, the same inlet, adapters and
converter serve the system's copy of Softland's code and a person's own codebase,
and both ends can be run over the same files to see the difference. [mine] The
base adds one thing to the road's read-back: from any layer but the base, a read
can show the base a batch behind (2.8; V52).

**6.2 Many writers.** The base is one line, decided in order on one task, every
write copied to every task, and "Nothing is built for a microbatch layer with many
writers" (`src-store/README.md:874-876`). If Sid, a few people, agents and the
ingester all write the code's facts into the base, it becomes that layer. A sharper
problem comes before any scale: two writers of one place. The ingester follows
git; a person or an agent edits the same place in the medium. If the ingester's
next act names, in `:replaces`, the fact that it last wrote, a gate refuses it as
`:replaced-fact-not-current` once someone else has written there
(`decision.clj:249-257`); if it names nothing, it writes over the edit without a
word, since a fact that names nothing to replace "can never be stale"
(`decision.clj:233-240`). So "who acts in the base" also asks whether a place has
one writer. P5, with the base written only by the ingester and every edit in a
layer above, gives each place one writer, and the read shows the edit over the
commit (2.8). [mine]

**6.3 Many readers.** Everyone reads the base on her own task (13 October
entry 4). What everyone should read belongs there; what a team reads can sit in
any streaming layer. Whether everyone in Softland reads Softland's own code is the
product half of K14, and it is open (`OPEN-QUESTIONS.md:120`).

**6.4 Years of data.** The log is never trimmed (‹727f622d›). At this repository's
pace, 149 commits in 14 days (2.10), the base's history of Softland's code grows by
each changed file of each commit, read in again as acts [not checked by a run].
Whether those years are useful turns on attribution: with W1 the base can say what
changed and never who changed it, nor in which run; with W2 to W4 and S1 or S3 it
can. That is the "how we got here" of the vision (`vision/LOG.md:56`), for the
medium's own code.

## 7. What hangs on what

- **Decision 1 (code or data).** If an adapter or a converter is a record, who
  wrote the record and who runs it are two ids: the act's `:who` is the runner,
  and the act can name the converter's facts in `:based-on`. [mine]
- **Decision 3 (the same thing across commits).** P5 needs entity ids that are the
  same in two layers; one writer for each place (6.2) is decided between 3 and 4.
- **K14, V25.** P1 puts closed material into the layer that starts "everyone".
  Choosing the base for Softland's code also leans K14 towards a world that is not
  the base; he left open "whether the layer that many people write into is a
  separate layer from the base" (`src-store/README.md:874-876`).
- **K7, K8, K10.** W3, P3 and P4 need a person before sign-in, K8's join of an
  account to a person who exists, and ids that say nothing.
- **K13.** P3 or P4 now and the base later is K13's road ("the other people's
  becomes the system one", ‹6d5e8c3c›): the content written again into a layer of
  the system's, or the system's layers building on it (`OPEN-QUESTIONS.md:119`).
- **E2, V10, V11.** Whoever holds "see and act" in the base can give and can
  change the base's settings, and nothing ends a permission; W2 and W4 add holders.
- **E3, E5, E6, W12 box C, Z4.** What a session means; whether a fact's session is
  within reach; whose reads are kept turns on the kind of actor, which no fact
  states yet.
- **V23.** Whether the road writes the first real record decides whether any of
  this costs anything yet.

## 8. My position, as defaults with reasons

Defaults for Sid to strike; none is decided.

**8.1 For the road.**

- (a) A run takes its actor, permission, layer and session as inputs; none is a
  constant of the converter. Reason: the road is an instance of both concerns
  (1.4), and its code is what reaches the first real record (section 5).
- (b) The system makes the base and gives one ingester id a permission beneath its
  own; the ingester writes the repository's core (the walk's two files, and
  `src-store/` with its page of decisions) into the base under that id (P6 with
  W2). Reasons: his base of 13 October is "the code that we are writing"
  (‹4123e23c›); system-level work "comes under that part" (‹ddf13d17›); a named
  actor keeps more than `:system` for the price of one act (‹5ff89890›), and a
  read can give it today, where W1 with S7 would give "system"; K9 shrinks to the
  start.
- (c) A fresh random session id for each run (S1). What the run read (commits,
  paths, which adapter) goes into facts or `:based-on`, never into an id. Reasons:
  4.3; runs 9 and 15.
- (d) The same code run once more over the same files, as a test person (a UUID)
  into her own layer that builds on the base, on the same store that is not kept.
  Reason: it proves the case of every other inhabitant, at the cost of one run if
  (a) holds.
- (e) Proposals, history, research, the vision log and memory stay out of the
  base: not read in on this road, or only into (d)'s layer. Reasons: P1's costs,
  and ‹60c1bcaa›.

**8.2 For the first real record**, which waits for his word: whether Softland's
code goes into the base, and what the base shows to a person who is not signed in
(K14, V25); P5 as the shape for edits made inside the medium (6.2); whether he acts
as himself (W3), which needs K8's join first; K9-b or K9-c before any program
outside his machines can reach the door.

**8.3 What I would not do.** W1 with S4 or S5. It is the cheapest tonight and keeps
the least: from the first real record on, each of its acts would say "the system",
in a session that groups nothing, for good.

## 9. Where I am unsure, and what I did not check

- Whether "my own layer" in the prompt means his personal layer, made by the
  system at his first sign-in, or a layer of his work (P3 against P4).
- Whether the first road's store is kept (V23). Sections 5 and 8 turn on it.
- Whether ‹6d5e8c3c› is his typing: the record cannot tell; it reads as dictation
  and is first in the record there.
- Whose wording `vision/LOG.md:226-241` and `vision/LOG.md:146` are: the record
  gives the first to a session's text and the second to a long prompt of his; the
  project's own page calls the log his depot.
- That "spoken" in ‹1088472f› is "signing" misheard.
- What writing the whole repository into the base costs on every task: not
  measured. The proofs' checks that I cite: read, not run by me.
- What an agent's id is (E7): no file says, and I did not try to answer it.
- No message from any agent other than the orchestrator reached me.

## 10. The questions this leaves for Sid, in everyday words

1. When the reader program brings Softland's own code into the store, should the
   record say "the system did it", or name the reader, as a helper that acts under
   the system? Example: next month you ask who wrote the fact "stamp.clj says X";
   the first gives "the system", the second "the repository reader, in the run of
   15 October". Default: name the reader.
2. Is Softland's own code part of the base that everyone will see, or kept to you
   and your agents until you open it? Example: a stranger signs in next year; does
   she see `src-store/`, and your proposals? Default: only the core goes into the
   base, and what the base shows to strangers is decided before anyone outside
   your machines can read it.
3. Your edits to code made inside Softland, and your notes: in the base beside the
   commit, or in your own layer above it, shown over the commit? Example: you
   rewrite a docstring inside Softland, and the next commit changes the same
   function. Default: your layer above; the base only follows the commits.
4. Before sign-in exists, may your person id be made by hand, if sign-in will later
   join your Google account to it? Example: your notes of this week stay yours
   after your first sign-in, instead of belonging to a second you. Default: yes,
   once the join of an account to a person who exists (K8) is decided; until then,
   a test person on a store that is not kept.
5. For a program, what is a session: one for each run, or the session of whoever
   started it? Example: an agent runs the reader three times in one conversation;
   three sessions, or one? Default: one for each run, linked later to whoever
   started it.
