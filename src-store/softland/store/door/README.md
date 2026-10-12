# Door — the door's parts, and what arrives on a depot

[Parent: store](../README.md) · [Store source entry](../../../README.md).

The door is [door.clj](../door.clj), one folder up, beside [act.clj](../act.clj)
and [module.clj](../module.clj): the one piece of code by which a maker's act
goes into the store and its answer comes back. It is the only file of the door
that requires Rama. This folder holds what it stands on, in two groups of files
that are kept apart.

All of this folder is pure code and loads with Clojure alone.

**What the door and the gates agree on.** A gate requires these three. They say
the form of a thing as it arrives on a depot, and how a thing is given one text
and one digest. A gate checks each thing that arrives against the declaration,
and the gate is the authority: anything can append to a depot without the door.

| File | Responsibility | State and lifetime |
|---|---|---|
| [declaration.clj](declaration.clj) | The door's declaration: what arrives on a depot, as shapes and rules in `act.clj`'s language; the store's namespace and the store keys; the limits; the test for a stray; the check, whose last step makes the one text of each value and holds the bound on one value and the bound on one act. | None. |
| [text.clj](text.clj) | The one text of a thing: one function from anything to a string, the same in every process. | None. |
| [digest.clj](digest.clj) | The digest of a thing, SHA-256 over its one text. The door and the gates share it. | None. |

**The door's own pure parts.** No gate requires these three. They are the rules
of the door that need no cluster, split from [door.clj](../door.clj) so that the
[pure proof](../../../../test-store/softland/store/README.md) holds each rule as
a case.

| File | Responsibility | State and lifetime |
|---|---|---|
| [additions.clj](additions.clj) | What the door makes of one act: the tests of what its two additions need, and the thing to append, which is the maker's act with `:act-id` and `:depot` added. | None. |
| [wait.clj](wait.clj) | The numbers of a maker's wait for an answer: the longest wait that the door counts, and the sleep between two reads of a wait. | None. |
| [words.clj](words.clj) | Everything that the door says to a maker, as data: its own seven words, the four answers, the two acks. | None. |

## How the files relate

[text.clj](text.clj) is at the bottom. It requires only [the act](../act.clj),
for what a value is. [digest.clj](digest.clj) takes the digest of a thing over
that text. [declaration.clj](declaration.clj) declares what arrives, and reads
its shapes with the engine of [act.clj](../act.clj); in the last step of its
check it makes the text of each value with [text.clj](text.clj).

[words.clj](words.clj) and [wait.clj](wait.clj) require nothing.
[additions.clj](additions.clj) requires the declaration, for what an act-id can
hold, and the words. [door.clj](../door.clj) requires these three and the
digest; outside this folder it requires two pure rules, the depot that a layer
starts with ([first_act.clj](../gate/first_act.clj)) and the routing id with the
tests of what a caller passes to a read ([rows.clj](../record/rows.clj)).

## Boundaries

Nothing in this folder reads the record or calls Rama. Whether a permission
stands and whether a replaced fact is current need rows of the record: those
checks are [gate/](../gate/README.md)'s. The door's calls of Rama's client, its
clock and its waits are in [door.clj](../door.clj).

The door owns no state. It borrows four handles of Rama's client, two depots
and two queries, and holds one number, fixed at its opening. It keeps nothing
between two calls: an act-id and a digest that a maker kept read her answer
through any door that is opened on the module.

The door is not a check of the act, not sign-in, not a network edge, not a
reader of facts for a screen, and not a bell. It is in-process Clojure, and in
this build no maker uses it but its proof.

## Decisions this build stands on

Sid's decisions are in [the store source entry](../../../README.md), with his
reasons. The door stands on the act's decisions of 6 October (the one form is
the maker's act; the door's additions are its own declaration, which a gate
checks; the act holds plain values), on the store's of 7 October (two depots;
the answer is read from the record by act-id, and an ack is a signal), and on
those of 8 October (who acts, the stamp, the store's keys). The door's own
four, of 9 October, by row:

| Row | Decision | Where it shows in the door |
|---|---|---|
| ?4 A | A person's id is a UUID that says nothing; her sign-in accounts are joined to it by links | The door makes no person and no id for one. It carries `:who` as given. An act-id's uuid is random, made from nothing of the person |
| H1 A | One lock seals one value | Nowhere: entry 2 of 12 October sets it aside, and the source entry says what became of its code |
| K3 A | The system can hold many layers | The door treats `:system` as a who like any other and checks nothing about it |
| K4 B | The store's own keys carry the namespace `softland.store` | The door reads no key of a fact: it appends a fact under a store key as it appends any other. The table of the store keys is `store-keys` in [declaration.clj](declaration.clj), which a gate's check reads, and which also says which of them take a second write |

Before real data is kept (12 October), the entries that the door carries:

| Entry | Decision | Where it shows in the door |
|---|---|---|
| 2 | Every value is plain in the log, and the store keeps no locks | The thing that the door appends is the maker's act with `:act-id` and `:depot` added, each value as she gave it (`thing-to-append` in [additions.clj](additions.clj)). The door holds nothing between two calls |
| 7 | An act from a maker who may not act is written to the log, and then refused. The door checks nothing first | The door's three tests, and no other, in `reading` of [additions.clj](additions.clj): each is of what its own two additions need. It reads no who, no permission and no fact |

## Engineering choices

Each is the builder's, with its reason. None is a decision of Sid's, and each is
provisional. The reasons in full are in the docstrings that the lines name.

**Where the door sits.** [door.clj](../door.clj) is at the top of the tree, as
the contract places it, so the tree reads `door.clj` above `door/` as it reads
`module.clj` above `module/`. What of the door needs no cluster is pure: its
tests, the building of the thing to append, the numbers of a wait, a table of
words. So the pure parts are three namespaces here, and the alias `:store`
stays the guard that they need nothing but Clojure.

**One call that the contract does not name.** It gives the act-ids of a series
with one read of the layer's depot, where one call for each id would read once
for each, on the layer's task.

**The read of a layer's depot.** The door reads the layer's depot for each
act-id that it makes: one round trip and one read on the layer's task, for a
row that no act of this build changes. It keeps no copy of it between two
calls. A maker that gives its act-id back, or takes the ids of a series in one
call, pays the read once.

**The order of the steps of a send** ([door.clj](../door.clj)). Four steps.
The reading: the three tests, which need no cluster and come before the first
round trip. The act-id: the one that the maker gives back, or one made with one
read of the layer's depot. The thing to append, and its digest. One append. So
one send is one append, and one read when no act-id was given back; no send
waits for another send.

**What the door does not stop.** The door stops no act. An act whose maker may
not act, a value that is no value of the form, a value over the bound on one
value and an act with more facts than the limit are each appended, and a gate
refuses each under its own act-id with the gate's word. The door bounds no
size: through it a thing of any size that Rama's client writes is in the log.
Nothing paces a sender. One thing more ends in a word of the door with nothing
appended, beside its three tests: a thing that throws while the door takes its
digest. An append that throws ends in `:append-cleanly`, the one word that
cannot say that nothing was appended. A thing that has no end in any part holds
its own maker's thread in the digest's walk, and that call does not come back.
The namespace docstring of [door.clj](../door.clj) says each of these, and what
goes plain into a depot: everything.

**The acks.** The streaming depot is appended to with the ack level `:ack`, as
the contract says, and the microbatch depot with `:append-ack`: no stream
topology reads that depot, so the level says what is meant. A maker gets the
ack as one of two words, since the two say different things, and never as the
answer.

**The wait** ([wait.clj](wait.clj) holds its numbers). One read, and with a
bound one more each 200 ms until an answer is there; the last read starts at
the bound. At 200 ms an answer that is there after 0.9 seconds costs 6 reads
where a read each 20 ms costs 46, and each read that finds nothing is a round
trip to a task that a gate also needs. A read that is on its way when the bound
passes takes its own time; Rama's own time limit of a query bounds it. An
opener can give the sleep between two reads, and no other number.

**The words** ([words.clj](words.clj)). Seven, one for each thing that the door
could not do, each with whether nothing was appended for the act or the door
does not know. Where in the maker's thing the door stopped is carried as data.
A word of the door carries no word of a gate: a gate's word is in the act's own
answer.

**The test of a plain map** is written once more, as a private function of
[additions.clj](additions.clj). It is two of Clojure's own predicates, and the
declaration, a gate's batch and the act's check each hold the same line
privately or in line; one home for it would change the code of each. The pure
proof holds the door's copy against the declaration's test for a stray.

**An act-id's uuid** is the JVM's random UUID, as the act's decision says: it is
made from nothing of the time. No read of this build ranges over act uuids.

**How each of the door's builder's rules is met.** The numbers are the
contract's.

1. The door checks nothing of the act's form. What it tests is what its two
   additions need: a plain map whose `:layer` can be read, no `:act-id` and no
   `:depot` already there, and a layer that an act-id can hold, with an act-id
   given back that is of that layer. Its words name what the door could not do.
2. An act-id carries a random uuid, and the door keeps no id that it gave.
3. The contract's rule is of a request for locks. The door sends no act of its
   own, so nothing here meets it.
4. The ack is given as a signal. The answer is read from the record by act-id,
   and the door compares the answer's digest with the digest that the maker
   gives back. A maker's second send of one act under one act-id is the same
   thing, with one digest, so both sends read the answer as their own; "not
   mine" is said only of a thing that differs.
5. A wait is bounded by the maker's word and is one question about one act-id.
6. The door reads no fact of the record for a maker.
7. The words are below.
8. In the runtime the module has two clients: the door, and
   [the read path](../read_path.clj), which reads the facts back out. Neither
   calls the other. The proofs that read or append round them say so in
   [their map](../../../../test-store/softland/store/README.md).

## Words a session made, Sid's to change

Each says what the thing is. The build took none from the rig's code. Carried
from the contract and the decisions, not made here: `:act-id`, `:depot`,
`:digest`, `:stamp`, `:reason`, `:accepted`, `:refused`; the calls `act-id-for`
and `answer-of`; "nothing yet" and "not mine".

**Namespaces.** `softland.store.door`, the contract's. `softland.store.door.additions`:
"additions" is the contract's word for what the door adds to an act, which is
two things, `:act-id` and `:depot`. `softland.store.door.wait`: the numbers of a
maker's wait for an answer. `softland.store.door.words`, and the door's proof,
`softland.store.door-proof`.

**Calls.**

| Word | What it is |
|---|---|
| `open` | Opens the door on a cluster handle and the module's name |
| `act-ids-for` | The act-ids of a series in one layer, with one read |
| `send-act` | Takes a maker's act into its depot |
| `readable-hex-of`, in [digest.clj](digest.clj) | The digest of a thing as hex, or nil for a thing that throws while it is read. The door takes its digest with it, so that such a thing ends in `:read-the-act` with nothing appended |

**The door's number.**

| Word | What it is |
|---|---|
| `:read-every-ms` | How long a wait sleeps between two reads of an answer. 200. The one number that an opener can give |
| `longest-wait-ms`, in [wait.clj](wait.clj) | The longest wait that the door counts, about 73 years; a longer one that an opener or a maker gives is cut to it |

**What a call gives.**

| Word | What it is |
|---|---|
| `:ack`, with `:gate-ran` and `:depot-holds` | The signal of the append. The streaming gate has processed the thing; or the microbatch depot holds it. Never the answer |
| `:answer`, with `:accepted`, `:refused`, `:nothing-yet`, `:not-mine` | What `answer-of` gives. The last two are the contract's phrases as keywords |
| `:could-not` | The name under which a result holds one of the door's own words |
| `:appended`, with `:nothing` and `:not-known` | Whether nothing was appended for the act, or the door does not know whether the depot holds it |
| `:at` | The place in the maker's thing where the door stopped, by the name of a part of the act; `:count` for the count of a series |
| `:module`, `:layer` | Names that a word carries: the module that the door was to be opened on, and the layer whose depot it could not read. `:append-cleanly` carries the act-id and the digest too |
| `:threw`, `:said` | The class of what Rama's client threw, and its message |

**The door's seven words.** Each reads after "the door could not":
`:find-the-module`, `:make-an-act-id`, `:read-the-layers-depot`, `:read-the-act`,
`:add-its-parts`, `:append-cleanly`, `:read-the-answer`.
[words.clj](words.clj) says what each is, with `:from` and `:says`.

**A reading**, with `:thing` and `:layer`: what the door read of a maker's thing
before any round trip, which is the thing and its layer.
