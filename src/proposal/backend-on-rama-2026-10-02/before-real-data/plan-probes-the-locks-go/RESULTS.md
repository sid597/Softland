# The probe of the plan of `the-locks-go`: results

Run on 12 and 13 October 2026 by the session that wrote `../PLAN-the-locks-go.md`, on one machine:
Clojure 1.12.4, Java 21.0.12.1, Rama 1.6.0 in-process, one worker, four tasks. A probe: no form, name
or table of it is a proposal for the store. It calls the store's own pure functions from
`src-store` (`softland.store.act`, `softland.store.door.text`, `softland.store.door.digest`, and
`softland.store.gate.batch/first-entity` as a partitioner) and changes no file of the store.

| Run | Command, from this folder | Log | What it asks |
|---|---|---|---|
| digests | `flock ../runs/cluster.lock clojure -M:digests` | `logs/digests.log` | Ruling 19's condition: is the digest of a thing as a client appended it the digest of the thing as a topology takes it off the depot? |
| sizes | `clojure -M:sizes` (pure, no cluster) | `logs/sizes.log` | The counts and times of section D of the plan |
| deep | `flock ../runs/cluster.lock timeout 420 clojure -M:deep` | `logs/deep.log` | What Rama's client writes and a topology reads of a thing that is very large or very deep |

The files `logs/*-stdout.txt` hold each run's whole output with Rama's own lines; a session reads the
`.log` files. Source: `src/probe/seen.clj` (what is said of one thing), `digests.clj` (the module and
the cases), `sizes.clj`, `deep.clj`.

## The module of the run

Two depots with the shapes of the store's two partitioners: one placed by the hash of the thing's
`:layer` and read by a stream topology (retry mode "all after", as the streaming gate's), one placed
by `softland.store.gate.batch/first-entity` and read by a microbatch topology. Each topology calls
`probe.seen/seen-of` on the thing as it came off the depot and keeps what it gives in a row. The
client calls the same function on the thing before the append, and on the record that it reads back
from the depot's log with `foreign-depot-read`. Each thing is a maker's act with `:act-id` and
`:depot` added and nothing else changed, which is the form of ruling 19.

## Ruling 19's condition: found

**The three digests are equal for every thing that was appended: 138 things, each through both
depots, 276 appends. No case differed** (`logs/digests.log`, the lines `SUM`; a search of the log
for `digest: false` finds none).

| Group | Cases | Appended | Door's digest = the topology's | = the digest of the record that a client read back | A class changed on the way |
|---|---|---|---|---|---|
| V, a value of each kind as a fact's `:v` | 83 | 83 | 83 on each depot | 83 on each | 3 |
| F, `:facts` in each form | 18 | 18 | 18 | 18 | 7 |
| A, parts of the act that are no value | 14 | 14 | 14 | 14 | 0 |
| N, kinds that the form does not take | 24 | 23 | 23 | 23 | 1 |

Group V holds: nil; true and false; eleven strings, among them the empty one, one with U+0000, one
that is U+0000 alone, one with U+1D11E, U+1F600 and U+10FFFF, one with both, one with U+FFFF, U+FEFF
and U+2028, and three of 65,536, 200,000 and 60,000 UTF-16 units; three keywords; three UUIDs; eight
fixed-width integers with both ends of the long range and an Integer, a Short and a Byte; seven big
integers on both sides of the long range, as `BigInt` and as `java.math.BigInteger`; eleven doubles
with -0.0, the least and the greatest; eleven decimals with trailing zeros, a zero with a scale and
exponents of 400; each collection empty, with nil inside, sorted, as a subvector, of 100 entries; and
values inside 32 collections. Two of the 83 are no values of the form by this session's own slip: a
vector and a map that hold the keyword `:1`, which does not read back. Their digests agree too, and a
gate would refuse them `:malformed`. So 81 are values that the form takes.

What changed on the way, with the digest the same in each case, because the one text is blind to it:

- **A lazy sequence and a `Cons` come off the depot as a chunked sequence**
  (`clojure.lang.PersistentVector$ChunkedSeq`). A list stays a list, a vector a vector, an empty lazy
  sequence an empty lazy sequence. The one text writes each sequence that is no vector as `( … )`, so
  the digest is the same. `:facts` given as a vector, a list, a lazy sequence not yet walked, one
  walked before, one made by `concat`, one made by `lazy-seq` and `cons`, `(seq vector)`,
  `(rest vector)`, a `Cons`, a subvector, a lazy sequence of 40 facts, and each empty form: equal.
- **A subvector comes off as a vector**, and an array map of nine entries as a hash map.
- For every case of V, F and A the record read back is `=` to the thing that was appended.

**One thing is changed in its value, and the digest does not see it: a string that is not well-formed
UTF-16.** A string with a lone surrogate (U+D83D, or U+DE00) is a value of the form (`value?` is
true). It comes off the depot with `?` in the place of the surrogate: `"a\uD83Db"` arrives as
`"a?b"`. The digest is equal all the same, because the digest is taken of the UTF-8 of the one text,
and the JVM's UTF-8 writes a lone surrogate as `?` too. So such a maker reads her answer as her own,
and the log and the row hold another string than she gave. This is not new with the locks' leaving:
today the door seals `(text/utf8 …)` of the value, which makes the same replacement before the seal
(`logs/sizes.log`, S5: today's row would hold `"a?b"`). What is new is who makes it: Rama's writing of
a record, and no line of the store. A gate cannot see it: the surrogate is gone when the thing
arrives.

**What Rama's client does not write.** A thing that holds a plain `java.lang.Object`: the append
throws (`Serializer not defined for type class java.lang.Object`, from Rama's own Nippy), and nothing
is appended. A ratio, a Java float, a character, a symbol, NaN, a keyword that does not read back, a
run of bytes, a `java.util.Date`, an `ArrayList`, a `HashMap`, an array of longs and a record of the
probe's own are written and read back with their class; a gate refuses each `:malformed`, and the
digests agree.

## The large and the deep (`logs/deep.log`)

| Thing | Streaming-shaped depot | Microbatch-shaped depot |
|---|---|---|
| One value, a string of 1,000,000 characters | appended, 118 ms, digests equal | appended, 35 ms, equal |
| 256 facts, each a value whose text is 65,536 bytes: 16.8 MB | appended, 443 ms, equal | appended, 185 ms, equal |
| One value, a string of 20,000,000 characters | appended, 900 ms, equal | appended, 255 ms, equal |
| 100,000 facts with no value | appended, 426 ms, equal | appended, 138 ms, equal |
| A value inside 1,000, 5,000 and 20,000 vectors | appended, 8 to 23 ms, equal; the act's check says `:malformed` | the same |
| A value inside 100,000 and 1,000,000 vectors | not appended: Rama's client throws `CallbackException: Callback failure` after 20 to 35 ms | the same |

The time of a streaming append holds the topology's own walk of the thing. After the last case both
topologies still took a small thing. The JVM had `-Xss16m` on every thread, the client's and the
tasks'. On a real cluster a worker has its own stack: a thing that a client with a deep stack writes
and a worker with a shallow one cannot read was not made here, and is not ruled out.

## The counts of section D (`logs/sizes.log`)

Times are the middle of some runs in one JVM after a warm-up: a floor, not a measure of a gate.

**One value of 65,536 bytes of text, in microseconds.**

| Walk | A string | A vector of 9,362 longs | A map of 520 small maps (55,941 bytes) | `"hello"` |
|---|---|---|---|---|
| `value?` and `normal-value`, the engine's leaf | 0.5 | 244 | 374 | 0.0 |
| The one text made, `(text/text v)` | 359 | 233 | 1,259 | 0.1 |
| Its bytes counted by a walk that builds no whole text | 358 | 234 | 1,271 | 0.1 |
| The digest of a thing that holds the value plain | 397 | 313 | 1,328 | 4.5 |
| The value digest from the row's String | 48 | 28 | 24 | 1.9 |
| Today: the digest of the thing with the value sealed | 44 | 32 | 27 | 5.1 |
| Today: the opening of the seal | 472 | 452 | 390 | 3.1 |
| Today: the row's String from the opened bytes | 56 | 35 | 29 | 0.1 |

A counting walk through the store's `text/feed` costs what making the text costs: the time is in the
making of each piece, not in the joining of the pieces.

**A permission of each depth.** `#too-deep` is in the text of a permission alone from depth 65, in the
text of a thing that holds it as the act's own from depth 64, and in the text of a thing that holds it
as a fact's value from depth 62. `value?` is true to depth 32 and false at 33. With the shape's number
set to 32 for the run (by `with-redefs-fn` on the private var; the store's file is not changed): an
act whose own permission has depth 32 passes the act's check and one of depth 33 is `:malformed`; a
give of depth 32 passes the declaration's check and one of depth 33 is `:malformed`; a permission of
depth 32 with the fifth slot is a value and has the shape. An own permission of depth 100,000 is
refused in 32 microseconds: the engine's walk stops at depth 33, and the digest's at 65.

| Depth | Text of the permission, UUID ids | Who and permission in each row, UUID ids | × 256 facts | The same, ids of 256 characters | × 256 facts |
|---|---|---|---|---|---|
| 1 | 136 | 180 | 46,080 | 1,028 | 263,168 |
| 4 | 547 | 591 | 151,296 | 3,347 | 856,832 |
| 5 | 684 | 728 | 186,368 | 4,120 | 1,054,720 |
| 16 | 2,191 | 2,235 | 572,160 | 12,623 | 3,231,488 |
| 32 | 4,383 | 4,427 | 1,133,312 | 24,991 | 6,397,696 |

The text of a permission of depth 32 is made in 8 microseconds with UUID ids and in 83 with ids of 256
characters.

**The largest thing that today's limits take** (256 facts, each a value of 65,536 bytes of text, with
entities and keys of 256 characters, each naming a fact to replace; 1,024 entries of `:based-on`; 256
subjects; an own permission of depth 4 with ids of 256 characters): its one text is 17,416,016 bytes;
its values are 16,777,216; the text that an accept writes into the rows of its facts, values and who
and permission for each fact, is 17,634,048. Under a permission of depth 32 that sum is 23,174,912.
The digest of that thing takes 95 ms, the making of its 256 row Strings 85 ms, and the text of who and
permission made 256 times 3 ms at depth 4 and 22 ms at depth 32.

**Two more, seen on the way.** A big integer that fits a long has the text `5N` as given and `5` in
its normal form, so the text of a value as given is not always the text that its row holds. And two
decimals that differ only in trailing zeros are one key of a map and one member of a set in Clojure
itself, so the normal form drops nothing there.

## Not settled by these runs

- A real cluster, two machines, a worker with another stack than its client, a restart, a change of
  leader.
- A thing with no end: no case was sent. Reasoned from `softland.store.door.text` ("A sequence with no
  end does not come back from it"): the door's digest does not come back for it.
- Whether a later Rama writes a lone surrogate in another way.
- Each case ran once.
