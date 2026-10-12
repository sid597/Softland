# Door — the door's parts, and what arrives on a depot

[Parent: store](../README.md) · [Store source entry](../../../README.md).

The door is [door.clj](../door.clj), one folder up, beside [act.clj](../act.clj)
and [module.clj](../module.clj): the one piece of code by which a maker's act
goes into the store and its answer comes back. It is the only file of the door
that requires Rama. This folder holds what it stands on, in two groups of files
that are kept apart.

All of this folder is pure code and loads with Clojure alone.

**What the door and the gates agree on.** A gate requires these four. They say
the form of a thing as it arrives on a depot, how a value is sealed to its
place, and how a thing is given one text and one digest. A gate checks each
thing that arrives against the declaration, and the gate is the authority:
anything can append to a depot without the door.

| File | Responsibility | State and lifetime |
|---|---|---|
| [declaration.clj](declaration.clj) | The door's declaration: what arrives on a depot, as shapes and rules in `act.clj`'s language; the store's namespace and the store keys; the limits; the test for a stray; the check. | None. |
| [seal.clj](seal.clj) | Locks and the seal: a lock's bytes, sealing a value to its place, opening it, and the length of a seal. | One random source for each process. |
| [text.clj](text.clj) | The one text of a thing: one function from anything to a string, the same in every process. | None. |
| [digest.clj](digest.clj) | The digest of a thing, SHA-256 over its one text. The door and the gates share it. | None. |

**The door's own pure parts.** No gate requires these three. They are the rules
of the door that need no cluster, split from [door.clj](../door.clj) so that the
[pure proof](../../../../test-store/softland/store/README.md) holds each rule as
a case.

| File | Responsibility | State and lifetime |
|---|---|---|
| [additions.clj](additions.clj) | What the door makes of one act: the tests of its own additions, the walk over the facts, the thing to append, and the request for locks. | None. A seal takes random bytes from the JVM. |
| [stock.clj](stock.clj) | The stock of locks as a value: its runs, the taking of the locks of one act, how many a request asks for, the one open request, and the door's five numbers. | None. The door holds each stock, in the memory of its process. |
| [words.clj](words.clj) | Everything that the door says to a maker, as data: its own fourteen words, the four answers, the two acks. | None. |

## How the files relate

[text.clj](text.clj) is at the bottom. It requires only [the act](../act.clj),
for what a value is. [digest.clj](digest.clj) takes the digest of a thing over
that text, and [seal.clj](seal.clj) binds a sealed value to the text of its
place. [declaration.clj](declaration.clj) declares what arrives, and reads its
shapes with the engine of [act.clj](../act.clj); it requires none of the other
three.

[words.clj](words.clj) requires nothing. [stock.clj](stock.clj) requires the
declaration, for the ceiling of one request, and the seal, for whether a lock's
way of sealing is known. [additions.clj](additions.clj) requires the act, the
declaration, the seal, the text and the words. [door.clj](../door.clj) requires
these three and the digest; outside this folder it requires two pure rules, the
depot that a layer starts with ([first_act.clj](../gate/first_act.clj)) and the
routing id with the tests of what a caller passes to a read
([rows.clj](../record/rows.clj)).

## Boundaries

Nothing in this folder reads the record or calls Rama. Whether a permission
stands, whether a replaced fact is current and whether a lock opens a sealed
value need rows of the record: those checks are [gate/](../gate/README.md)'s.
Nothing here writes a lock anywhere: a gate writes a lock's row, in
[module/](../module/README.md). The door's calls of Rama's client, its clock, its
waits and the seat of a stock are in [door.clj](../door.clj).

The door is not a check of the act, not sign-in, not a network edge, not a
reader of facts for a screen, and not a bell. It is in-process Clojure, and in
this build no maker uses it but its proof.

## Decisions this build stands on

Sid's decisions are in [the store source entry](../../../README.md), with his
reasons. The door stands on the act's decisions of 6 October (the one form is
the maker's act; the door's additions are its own declaration, which a gate
checks; the seal is below the form), on the store's of 7 October (two depots;
the answer is read from the record by act-id, and an ack is a signal), and on
those of 8 October (who acts, the stamp, the store's keys). The door's own
four, of 9 October, by row:

| Row | Decision | Where it shows in the door |
|---|---|---|
| ?4 A | A person's id is a UUID that says nothing; her sign-in accounts are joined to it by links | The door makes no person and no id for one. It carries `:who` as given. An act-id's uuid is random, made from nothing of the person |
| H1 A | One lock seals one value | The door takes one lock from a stock for each value fact and seals the value with it. The switch of a layer to one lock for each act is not built |
| K3 A | The system can hold many layers | The door treats `:system` as a who like any other and checks nothing about it |
| K4 B | The store's own keys carry the namespace `softland.store` | The door knows the store keys for one reason: a value under one of them stays plain. The table of them is `store-keys` in [declaration.clj](declaration.clj), which also says which of them take a second write. The door asks for locks under `:softland.store/lock-request` |

## Engineering choices

Each is the builder's, with its reason. None is a decision of Sid's, and each is
provisional. The reasons in full are in the docstrings that the lines name.

**Where the door sits.** [door.clj](../door.clj) is at the top of the tree, as
the contract places it, so the tree reads `door.clj` above `door/` as it reads
`module.clj` above `module/`. Nearly all of the door is pure: tests, a walk over
facts, the building of two maps, arithmetic on a queue, a table of words. So the
pure parts are three namespaces here, and the alias `:store` stays the guard
that they need nothing but Clojure.

**Two calls that the contract does not name.** One gives the act-ids of a
series with one read of the layer's depot, where one call for each id would
read once for each, on the layer's task. One says how many unused locks the
door holds, as numbers and never as bytes, so that an operator can see what the
end of the process would leave.

**The stock** ([stock.clj](stock.clj); the getting of locks in
[door.clj](../door.clj)).

- A stock is for one layer, one depot and one who. The contract's rule is the
  layer and the who: a lock is used only for values of acts of the who that
  asked for it, in that layer, so that a forget of a person can find her locks
  by her requests. The depot is added because a gate holds a lock to its layer
  and its depot: an act under an act-id of the wrong depot then finds an empty
  stock and costs no lock.
- The door asks for locks only at need, inside the call of a maker whose act
  needs more than its stock holds. It has no thread of its own, and a request
  that no call waits on would have nobody to hand a refusal to.
- A request asks for as many locks as its stock has given out so far: 4 at
  least, at most 64 in a streaming layer and 256 in a microbatch layer, and
  never fewer than the act needs. A request costs the cluster about as much as
  five unused locks, and what a restart loses for a who is then fewer locks
  than its last request asked for.
- One call at a time asks for one stock; the call sits in the stock's seat.
  Another maker that needs locks of that stock waits for the seat. Taking
  locks never waits. Without the seat, each maker that found the stock short
  would send its own request, and in the base, where a request takes about a
  second, the stock would have no bound.
- One time limit for all of a call's getting of locks, 10 seconds by default.
  It says when the door stops waiting for the seat and stops starting a step;
  the call in the seat can outlast it by the one call of Rama's client that
  was on its way, an append or a query.
- The seat is held for the door's own steps and for calls of Rama's client,
  and for nothing whose length a maker's thing sets. The call in the seat
  takes the digest of its request, which holds four parts of the maker's act
  as given; so a call walks those parts once before it goes for the seat. A
  part that does not come back when it is read then holds its own maker's
  thread and no other maker.
- A maker is handed the refusal only of a request that its own call sent. One
  call sends one request at most.
- A request is written into its stock before it is appended. One whose append
  threw is kept as in doubt and read by the next call that needs the stock,
  because an append that threw may have gone through. It is forgotten only at
  a read that finds nothing 10 seconds or more after the throw. The door sends
  nothing a second time.
- A read of a request's locks that came back ends the request, whatever it
  gave. The stock keeps no answer that was read: after a read of locks that
  threw, the next call reads the answer again.
- The locks of one act are taken in one step, the oldest run first, across the
  border of two runs. An act names one request, or two at a border, and
  nothing is left behind in an old run. It names more than two only where a
  call read the locks of a request that an earlier call left open, was still
  short, and asked for its own: one request for each run that the act crosses.
- A lock that was taken never comes back, whatever becomes of its act. So each
  value of an act that is refused, that arrives second under its act-id, or
  whose append threw, leaves one row that seals nothing.
- A stock that holds nothing at all leaves the door's memory when the last
  call that used it leaves. A stock that gave out a lock stays for the life of
  the process. A stock that holds an open request stays until a later call of
  its who reads that request or forgets it; a `:who` that is a plain Java
  object is equal to no other, so each call under a new one whose request's
  append threw leaves a stock for good.

**What the stock costs the base.** One who gets at most 256 locks for each time
that a request's batch takes to close, and each send that finds its stock short
waits that time. A large import by one who into the base is the case that
meets it; the switch of a layer to one lock for each act is kept for such a
layer and is not built.

**The read of a layer's depot.** The door reads the layer's depot for each
act-id that it makes: one round trip and one read on the layer's task, for a
row that no act of this build changes. It keeps no copy of it between two
calls. A maker that gives its act-id back, or takes the ids of a series in one
call, pays the read once.

**The order of the steps of a send** ([door.clj](../door.clj)). Every test that
needs no cluster comes before the first round trip, and every step that can
fail without a lock comes before the taking of locks. The limit of sealed bytes
is tested on the plain bytes before a lock is taken, by a length that
[seal.clj](seal.clj) gives, and on the sealed bytes after.

**The acks.** The streaming depot is appended to with the ack level `:ack`, as
the contract says, and the microbatch depot with `:append-ack`: no stream
topology reads that depot, so the level says what is meant. A maker gets the
ack as one of two words, since the two say different things, and never as the
answer.

**The wait.** One read, and with a bound one more each 200 ms until an answer
is there; the last read starts at the bound. At 200 ms an answer that is there
after 0.9 seconds costs 6 reads where a read each 20 ms costs 46, and each read
that finds nothing is a round trip to a task that a gate also needs. A read
that is on its way when the bound passes takes its own time; Rama's own time
limit of a query bounds it.

**The words** ([words.clj](words.clj)). Fourteen, one for each thing that the
door could not do, each with whether nothing was appended for the act or the
door does not know. Where in the maker's thing the door stopped is carried as
data. A word about a request says what became of the request.

**The test of a plain map** is written once more, as a private function of
[additions.clj](additions.clj). It is two of Clojure's own predicates, and the
declaration, a gate's batch and the act's check each hold the same line
privately or in line; one home for it would change the code of each. The pure
proof holds the door's copy against the declaration's test for a stray.

**An act-id's uuid** is the JVM's random UUID, as the act's decision says: it is
made from nothing of the time. No read of this build ranges over act uuids.

**How each of the door's builder's rules is met.** The numbers are the
contract's.

1. The door checks nothing of the act's form. What it tests is what its own
   additions need: a plain map, an id that an act-id can hold, parts that it
   would write over, a value that reads back once sealed, and two limits of its
   own steps. Its words name what the door could not do. This narrows the rule
   by one corner, the form of a value: after the seal no gate can hold it.
2. An act-id carries a random uuid, and the door keeps no id that it gave.
3. A request for locks carries the maker's who and permission, and a stock is
   for one who in one layer. A restart asks again.
4. The ack is given as a signal. The answer is read from the record by act-id,
   and the door compares the answer's digest with the digest that the maker
   gives back. It reads a request's answer in the same way before its locks.
5. A wait is bounded by the maker's word and is one question about one act-id.
6. The door never opens a sealed value, reads no fact of the record for a
   maker, and reads the locks only of requests that it made in this process.
7. The words are below.
8. In the runtime the module has two clients: the door, and
   [the read path](../read_path.clj), which reads the facts back out. Neither
   calls the other. The proofs that read or append round them say so in
   [their map](../../../../test-store/softland/store/README.md).

## Words a session made, Sid's to change

Each says what the thing is. The build took none from the rig's code; of one
word that it was handed, "stock", that is not known (below). Carried from the
contract and the decisions, not made here: `:act-id`, `:depot`, `:sealed`,
`:lock-id`, `:digest`, `:stamp`, `:reason`, `:accepted`, `:refused`; the calls
`act-id-for` and `answer-of`; "nothing yet" and "not mine".

**Namespaces.** `softland.store.door`, the contract's. `softland.store.door.additions`:
"additions" is the contract's word for the three things that the door adds to
an act. `softland.store.door.stock`, `softland.store.door.words`, and the
door's proof, `softland.store.door-proof`.

**Calls.**

| Word | What it is |
|---|---|
| `open` | Opens the door on a cluster handle and the module's name |
| `act-ids-for` | The act-ids of a series in one layer, with one read |
| `send-act` | Takes a maker's act into its depot |
| `stock-count` | How many unused locks the door holds, in how many stocks, and how many requests it has not read. Its three numbers are `:unused-locks`, `:stocks` and `:open-requests` |
| `sealed-length`, in [seal.clj](seal.clj) | How many bytes a seal gives for a count of plain bytes, by the way of sealing |

**The door's five numbers.**

| Word | What it is |
|---|---|
| `:request-wait-ms` | How long one call waits for locks, in all, before it ends in a word. 10,000 |
| `:read-every-ms` | How long a wait sleeps between two reads of an answer. 200 |
| `:fewest-at-once` | The fewest locks that the size rule asks for in one request. 4. An act that needs more gets more |
| `:most-at-once-streaming`, `:most-at-once-microbatch` | The most that the size rule asks for in one request, by the depot. 64 and 256 |
| `longest-wait-ms`, in [stock.clj](stock.clj) | The longest wait that the door counts, about 73 years; a longer one that an opener or a maker gives is cut to it |

**What a call gives.**

| Word | What it is |
|---|---|
| `:ack`, with `:gate-ran` and `:depot-holds` | The signal of the append. The streaming gate has processed the thing; or the microbatch depot holds it. Never the answer |
| `:answer`, with `:accepted`, `:refused`, `:nothing-yet`, `:not-mine` | What `answer-of` gives. The last two are the contract's phrases as keywords |
| `:could-not` | The name under which a result holds one of the door's own words |
| `:appended`, with `:nothing` and `:not-known` | Whether nothing was appended for the act, or the door does not know whether the depot holds it |
| `:at` | The place in the maker's thing where the door stopped, by the thing's own part names; `:count` for the count of a series |
| `:most`, `:sealed-bytes`, `:waited-ms`, `:module`, `:layer` | Numbers and names that a word carries |
| `:request`, `:request-answer` | The act-id of the door's request for locks, and its answer as `answer-of` gives one |
| `:threw`, `:said` | The class of what Rama's client threw, and its message |

**The door's fourteen words.** Each reads after "the door could not":
`:find-the-module`, `:make-an-act-id`, `:read-the-layers-depot`, `:read-the-act`,
`:add-its-parts`, `:seal-a-value`, `:seal-that-many-values`,
`:have-its-request-accepted`, `:append-its-request-cleanly`,
`:get-locks-within-its-wait`, `:read-its-requests-answer`,
`:read-its-requests-locks`, `:append-cleanly`, `:read-the-answer`.
[words.clj](words.clj) says what each is, with `:from` and `:says`.

**Words of the stock**, in the explanations and in the names of the code.

| Word | What it is |
|---|---|
| a stock | The unused locks that the door holds for one who in one layer and that layer's depot. The door's memory is its stocks. The word is the build contract's. It is a session's word and not Sid's, and the rig's code has a function `stock!`; whether the word came from there is not known |
| a run, `:runs` | The unused locks of one request, in the order of their indexes |
| open request, `:open` | A request that the door sent and whose locks it has not read. Its states, under `:state`: `:sent`, `:in-doubt` |
| in doubt, `:since` | Of a request: its append threw, or has not returned yet, so the door does not know whether a depot holds it; and the moment from which it is kept |
| seat | What lets one call at a time ask for a stock. Never called a lock |
| given out, `:given-out` | Of a stock: how many locks it has handed to acts since the door was opened |
| a reading, with `:thing`, `:carried`, `:entries`, `:values` | What the door read of a maker's thing before any round trip: the thing, its layer, its who, the four parts that a request for locks carries, one entry for each fact, and how many values there are to seal. An entry holds `:fact`, and for a value fact `:plain` and `:place` |
| `:calls`, `:cell` | Of a stock in the door's memory, beside its seat: how many calls are using it now, and the place that holds its value |
| `:lock` | Beside `:lock-id`, in a lock as a stock gives it to an act: the lock's bytes and its way |
| value fact | A fact that carries a value and whose key is not one of the store keys: a fact whose value the door seals |
