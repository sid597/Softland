# Plan: the read path — a query as data, a read through layers, once or kept live

**What this paper is.** The plan of the part `read-path` of the read build: the contract's B1 to B5, its
rules R1 to R5, and the measures of its section C (`../BUILD-reads.md`), as rulings 8, 9, 11 to 17, 23 to
31, 36, 37, 38 and 40 to 45 of `PROGRESS.md` read them. It designs one piece of code in the runtime's
process, a client of the store's module as the door is. It writes no code of the store.
**The stage.** Phase 1 of the Rama skill (`phase-1-plan.md`), for the last part of three
(`DECOMPOSITION.json`, the entry `read-path`).
**Who wrote it.** One fresh session (Opus 5.5), in the night of 11 to 12 October 2026, for the build
session to judge and a fresh session to check. It wrote this file and the folder
`plan-probes-read-path/`, and changed no other file. The tree was at head `a8c1783b` when it began and
at `a6857ba8` when it ended; in between, both plans before this one passed their checks and another
session began to change `src-store/` for their builds. Every form of the record below is taken from
`PLAN-what-the-gates-keep.md`, "The hand-off", as it stands after its check, and not from the code
(ruling 38). Line numbers of `src-store/` and `test-store/` are those of head `c3e65c06`. Rulings 46 to
59 came while this paper was written; 49, 53, 57 and 58 touch it and are taken in.

**Blocked.** Nothing.

**Changed by the check of this plan.** The check (`PLAN_VALIDATION-read-path.md`, 12 October 2026)
first applied ruling 64, which closes this plan's open point 5 the other way. It then found fifteen
faults and fixed each here; its paper gives the lines before and after. The largest: the argument
that no put-together value need be kept did not hold as "Live" worded its step 5 for what points at a
thing. It holds once two tests are made exact and a read holds one mark; no value is kept. The check
ran two things of its own (`plan-check-probes-read-path/`): "C1" to "C9" below are the cases of its
run on Rama's client, and "the model" is its play of every order of the callbacks.

**A ruling that cannot hold.** None. Three rulings are read, and each reading is said here so that the
build session can strike it.

- *Ruling 23* says that a live read follows each fact that shapes it "by a push of its own". Four of
  those facts are neighbours in the table: the layer's depot, its setting, its list and its marks all sit
  on the layer's own entity under keys of the store's namespace. One push on that run of at most six rows
  follows the four, with one seek where four pushes would cost four. So "a push of its own" is read as:
  each such fact is followed by a push that its askers share. The asker's permission has a push of its
  own.
- *Ruling 23* names the asker's permission among the facts that shape a read. While a layer's setting is
  "everyone" her permission shapes nothing. So her permission in a layer is followed only while that
  layer's setting is "members"; when the setting goes to "members" under an open read, the push on her
  permission is opened then. This saves one push for each asker of the base.
- *Ruling 30* says that whether a layer is made is read from its depot fact "only when the asker's own
  permission does not already say so". The depot fact is in the run above, so it is read at no seek more,
  and the plan reads it always. The ruling's economy is met without its condition.

**Does the part need a store of its own beside Rama's push (R2; the repository's rule on caching)?** No.
The section "Sharing, and what the read path holds" says what is held and shows that every value that a
read gives is worked out, at each change, from the values that Rama's pushes hold at that moment.

**Does the plan grow past what one session can check (ruling 37)?** No cut is made. A read asked once and
a read kept live share the pure core whole (the form, the layers, the sources, the answer); the live part
adds the sharing and the handling of a change. The cut of ruling 37 stays available: the sections "Live"
and "Sharing", the functions 30, 30b and 36 to 41, and the checks 14 to 21, 24 and 25 are the live
half.

**How the skill's template is adapted.** The template plans a module. This part adds no depot, no PState,
no topology and no query to the module (the contract's picture: "No query of the store stands between").
Every heading of the template is kept. "Reads" is each thing that the read path asks of Rama, with its
table, its partitioning key and its path. "Writes", "Depots", "Topologies and PStates" and "Query
Topologies" say that there are none, and why. "PState Design" says which table each word of a query
reads, as the part before designed them. "Partitioning efficiency" is filled for the reads. "State
primitive selection" and "Resource usage" are about the memory of the runtime's process. The task asks
for more than the template, and each has its own section: the form of a query, the calls, what a read
gives, the pure core, live, sharing, the tasks of a microbatch layer, a very large run, the scenarios by
hand, the cost, the proof, the measures, the functions in the order to build, the words and the numbers,
the explanations, what is not decided, and the open points.

**Marks.** "Read at" is read in the source at that line. "Run" names the run and its log. "Read, not run"
is a line of Rama's pages under `reference/rama/docs/`, an extract of March 2026 that names Rama 1.5.0,
where the store runs 1.6.0; or a docstring of the pinned jar. "Derived" is reasoned here from lines that
are named. A file name without a folder is under `src-store/softland/store/`. "The hand-off" is the
section of that name in `PLAN-what-the-gates-keep.md`. "The push probe" and "the batch probe" are
`push-probe/RESULTS.md` and `batch-probe/RESULTS.md`. "T" and "U" are this plan's two probes, below.

## What was run for this plan

Two probes, in `plan-probes-read-path/`. Each is a probe: its module is no code of the store, and no
form, name or table shape comes from it. Both on Rama 1.6.0, in-process, one machine, 4 tasks, 2 threads,
one watcher, small values, each case once or three times; the times include a warming JVM and no
network. Run again from that folder: `clojure -M:tasks`, `clojure -M:runs`; one at a time. The lines
that each printed are in `logs/tasks-run-1.log`, `logs/tasks-run-2.log` and `logs/runs-run-2.log`; the
files `logs/*-all.log` are the whole outputs with Rama's own lines.

**T, `src/probe/tasks.clj`: what a client can do.**

| # | Asked | Found |
|---|---|---|
| T1 | Does a client learn the number of tasks from a table's handle? | Yes. `(foreign-object-info pstate)` gave `{:name "$$m-rows" :module-name … :num-partitions 4}` for a table of a stream topology, for one of a microbatch topology, and for a depot |
| T2 | Does Rama give one key for each task, outside its test namespace? | Yes. `com.rpl.rama.Helpers/genHashingIndexKeys 4` gave `["d" "a" "b" "g"]`, four Strings, the same as `com.rpl.rama.test/gen-hashing-index-keys`, which the module's proof already uses (`module_proof.clj:269-278`, `:437-440`) |
| T3 | Does a select with such a key as `:pkey` read one task, and each task? | Yes, on both tables. With two rows written on each task, the select `[(sorted-map-range "L " "L!")]` with the key of task t gave the rows of task t and no other |
| T4 | Does a push with such a key watch one task, on a table that a microbatch topology writes? | Yes. Four pushes on the same run, one for each key. A write on task 2 called back the push of task 2 with one `KeyDiff`, and no other push |
| T5 | One event that changes a place, adds a place and removes a place of one run | One callback, a `MultiDiff`. A processor that implements `KeyDiff$Processor` and `KeyRemoveDiff$Processor` was handed each place: `[:put "L m a"] [:put "L m d"] [:out "L m b"]`. The same from one batch of a microbatch topology. A row written again with the same value: one callback, a `KeyDiff`, new value equal to the old. A push on one place: `NewValueDiff`; a removed row comes as `NewValueDiff[null]` and the value is nil; a place with no row opens with the value nil and gets its first row as `NewValueDiff` |
| T5 | The first callback of a push | A `ResyncDiff` whose old value is nil, on the push's own thread. Whether it runs before or after the opening call returns was not noted; the plan rests on neither |
| T5 | On which thread a callback runs | A thread of Rama's client named `clientside-cluster-manager-executor-pool-0-0` or `…-pool-1-0`. One push kept its thread; two pushes had two threads. So the callbacks of two pushes can run at the same moment |
| T6 | Closing a push | `close!` gives the callback one last call, a `DestroyedDiff` with a nil value. Then `proxy-status` is `:terminated`, `deref` throws "Tried to fetch from closed ProxyState", a write on the task calls nothing back, and a second `close!` throws nothing |
| T7 | The calls that do not block, with `:pkey` | `foreign-select-one-async` and `foreign-proxy-async` each took `{:pkey …}` and gave a `CompletableFuture`; the push's future was given in 1.3 ms |
| T8 | What a run costs the task, by its size (a run of 200,000 places, one task) | Counted on the task, `[(sorted-map-range from to) (view count)]`: 113 to 168 ms. The run and then its first 3: 127 ms, so a cut after the run does not shorten the walk. The first 3 from the run's start with no end, `(sorted-map-range-from from {:max-amt 3})`: 0.3 to 3.5 ms. The run whole: 252 ms |
| T8 | 2,000 places of one task in one call | `(foreign-select [(multi-path (keypath k1) … (keypath k2000))] …)`: 14 to 21 ms, 2,000 rows. One call for each, one after another: 269 ms. One call for each, all sent at once: 44 to 48 ms |
| T9 | Several paths in one call | `(foreign-select [(multi-path (sorted-map-range a b) (keypath k) (keypath none))] pstate {:pkey …})` gave three values in the paths' order: the run, the row, nil |
| T9 | **A path that Rama cannot serve can stop a worker** | Run 1: `(foreign-select-one [(submap [k1 k2])] …)` on a top-level table threw `AbstractMethodError` on the task's thread ("Uncaught throwable in task thread outer loop"), Rama logged "One or more monitored components have become unhealthy! Triggering system shutdown", the select got "CallbackException: Callback failure", a push on the same path did not open within 6 s, and the next append threw (`logs/tasks-run-1.log`, its last lines). It runs again only with `clojure -M:tasks submap` |

**U, `src/probe/runs.clj`: how a read of a run can be bounded in the path itself.**

| # | Asked | Found |
|---|---|---|
| U1 | The first n places from a run's start, and then the run's own end: `[(sorted-map-range-from from {:max-amt n}) (sorted-map-range from to)]` | With n = 257: the 3 places of a small run in 1.1 to 5.8 ms; the first 257 of a run of 200,000 in 2.1 to 4.2 ms. With n = 20,001 on a small run: the 3 places, in 16 to 36 ms, so the walk is of the window. With n = 20,001 on the run of 200,000: **it throws**, `IllegalArgumentException: No implementation of method: :combine-separated-sorted-structures*`, each time; the worker went on. So the form holds for a small window and not for a large result |
| U1 | The same, counted: `… (view count)` | 257 in 0.6 ms |
| U2 | The same inside a `multi-path`, beside two places | `[row nil {the 3 places}]`, in the paths' order |
| U3 | The same as a push (a window of 6 over a run of 3, the window reaching past the run's end) | It opens. A place of the run changed: one `KeyDiff`. A new place in the run: one `KeyDiff`. A place inside the window and after the run's end changed: no callback |
| U4 | A run whose two ends are the same text | An empty map |
| U4 | A run with its end included | By the option `{:inclusive-end? true}`, and by a space after the end's text: both gave the end's place |
| U5 | A run whose ends are in the wrong order | An empty map. The worker went on |
| U6 | How large a run is, asked with a bounded walk: `(foreign-select [(sorted-map-range-from from {:max-amt 20001}) (multi-path (view count) [LAST FIRST])] …)` | Two values, the count of the window and the key of its last place: `[20001 "L big k020000"]` in 10 to 32 ms; `[1 "L n x"]` for a window that reaches the end of the task; `[0]`, one value, for a window that holds nothing |
| U6 | A push on the run of 200,000, exact range | It opened in 298 ms with 200,000 rows in the client. Rama refuses nothing by size |

**What these settle.** How a client names one task (T1 to T4). How a change of a run names its places
(T5). What closing does (T6). That one call can carry several reads of one task (T9, U2). That a read of
a run can be bounded for a small run, and sized with a bounded walk for a large one (U1, U6). And two
things that shape the read path's guard: Rama serves a run of any size (U6), and one path form that Rama
does not serve stopped a worker (T9).

**What they do not settle.** Everything about the store's own module: the probes do not load it. A real
cluster, a network, replication, a move of a task's leader. Many pushes at once. What a push costs a
gate's write. Whether `Helpers/genHashingIndexKeys` is meant for a client outside a test (open point 3).
A `multi-path` of 2,000 paths gave 2,000 rows; that they came in the paths' order was looked at for three
paths only, so the build's proof compares it.

## The design in short

```
A CALLER in the runtime                      THE READ PATH                                  RAMA, the module Store
  a query, as data ───────────────▶ query/check ── a fault ──▶ a word, no call
                                        │ the query in its one form
                                        ▼
                    ┌────────── sources/reading ◀────────────── what is known so far
                    │   which layers, in order · may she see each ·
                    │   which SOURCES are wanted now · which are missing
                    │ missing
                    ▼
            asked once: one select for each task that holds a missing source ──▶ the four tables
            kept live:  one push for each missing source, shared by its key  ──▶ (two of facts, two of
                    │                                                              facts by value)
                    └── known grows; round again, at most 5 rounds
                                        │ nothing missing
                                        ▼
                                  answer/whole ──▶ the answer now
  kept live, at each callback of a push:
      the places that the diff names ──▶ answer/of-places, from what the pushes hold now ──▶ the asker's function
```

1. **A query is a plain map of a few words** (`softland.store.read-path.query`): what is asked, one of
   `:place`, `:entity`, `:run`, `:points-at`; who asks; a list of layers or a layer to take the list
   from; which parts of a row are asked for; whether the store's own facts are asked for. Once or live
   is the call that the caller makes, not a part of the data, so that one query can be asked both ways
   and two askers of one thing hold equal data (open point 1).
2. **A source is one value that Rama gives and can push**: one row, or one run of neighbouring rows, of
   one table on one task. As data it is the table, the partitioning key and the path. Two askers look
   at the same thing when their sources are equal maps; that map is the key by which a push is shared.
3. **One pure function says what a read needs next** from what it knows so far, and another says what
   the read gives. The code that calls Rama only fetches what is missing and asks again. The loop ends
   in at most 5 rounds (section "The pure core").
4. **Asked once**: every missing source of one task goes in one call, `foreign-select-async` with a
   `multi-path`. A place through three layers is three calls in one round.
5. **Kept live**: every source is one `foreign-proxy-async`. At a callback, the read path works out,
   for the places that the diff names, what the read gives from the values that the pushes hold at
   that moment, and calls the asker's function unless one of three tests shows that nothing changed
   for her ("Live"). It keeps no value of its own.
6. **Every path that the read path gives Rama is one of five forms that it builds itself** from texts
   that `record/rows.clj` makes. No caller gives a path. A run is read whole only after a bounded read
   has shown that it holds no more than the build's number of places (section "R3, and a very large
   run").

## Reads

Every read of Rama that the read path makes. "Layer's key" is the partitioning key of a source of that
layer: the hand-off's table "Which key a client gives Rama to reach a row". No read routes by a row's
whole key (implied spec F3).

| # | The source | Table | Path, as Rama's client is given it | Partitioning key | Asked once | Kept live |
|---|---|---|---|---|---|---|
| S1 | The layer's own rows: its depot, setting, list and marks | facts of the layer's gate | `(sorted-map-range own-from own-to)`: the run of the layer's own entity under the store's namespace, at most six rows | streaming: the layer. Microbatch: every task holds the rows; live, the layer; once, the key of another read of that layer in the same round, else the layer | in the layer's call | one push for a layer, shared by every asker of every read through it |
| S2 | The asker's permission in the layer | facts | `(keypath (rows/place-key layer asker :softland.store/permission))` | as S1 | in the layer's call | one push for an asker and a layer, only while the layer is made and its setting is "members" |
| S3 | A place | facts | `(keypath (rows/place-key layer entity key))` | streaming: the layer. Microbatch: the entity | in the layer's call | one push |
| S4 | A run: an entity, or any run of neighbouring places | facts | first `[(sorted-map-range-from from {:max-amt 257}) (sorted-map-range from to)]`; for a run of more than 256 places then the size, and then `(sorted-map-range from to)` | streaming: the layer. Microbatch, both ends in one entity: the entity. Microbatch, else: one source for each task, the key of task i | first read in the layer's call; a large run costs two calls more | one push on `(sorted-map-range from to)`, opened after the bounded reads |
| S5 | What points at a thing V under a key K that the layer marks since S | facts by value | as S4, with `(rows/by-value-run layer K S V)` as the two ends | streaming: the layer. Microbatch: `(rows/value-digest-of V)` | one round after S1, which gives S | as S4 |
| S6 | Ruling 12's check: which row, if any, does a nearer layer hold at a place that a farther layer's S5 gave (ruling 64 reads its value) | facts of the nearer layer | `(keypath (rows/place-key nearer entity K))` | streaming: the nearer layer. Microbatch: the entity | all of one task in one call, 2,000 paths at most in a call | one push for each place and nearer layer |
| S7 | The size of a run of more than 256 places | as its run | `[(sorted-map-range-from from {:max-amt (inc max)}) (multi-path (view count) [LAST FIRST])]` | as its run | a call of its own | a select, before the push is opened |
| S8 | How many tasks the microbatch gate's tables have | — | `(:num-partitions (foreign-object-info microbatch-facts))`, then `(Helpers/genHashingIndexKeys n)` | — | at each read that has a run across entities in a microbatch layer | the same |

None is a query topology, by the contract's picture and entry 3 of Sid's decisions: a push needs a
table that a client reads, and the pushed value is the read. The skill's rule that several reads of one
request go in one round trip is met for a read asked once by the `multi-path` of one call for each task
(run: T9, U2). It cannot be met across tasks or across layers of two tasks without a query of the store;
ruling 13 keeps that as it falls out.

## Writes

None. The read path appends to no depot and keeps no read ("this build keeps no read", the contract,
"Not in this build").

## PState Design

None is added or changed by this part. The four tables are the part before's, and the reasons for
their forms are in its "PState Design". What this part reads of each:

| A word of a query | Table | Why this read is one seek and a walk |
|---|---|---|
| a place | facts | one row under `place-key` |
| an entity | facts | the entity's places are neighbours: `"<layer> <entity> "` to `"<layer> <entity>!"` (hand-off) |
| a run | facts | the order of the keys is layer, entity, key; both ends are in one layer |
| what points at a thing | facts by value | one value's places under one key are neighbours, and in a microbatch layer on one task, found from the value alone (that plan, 5) |
| the layer's own rows | facts | the four store keys that sit on the layer's own entity are neighbours: every key of the store's namespace prints as `:softland.store/…`, and the next text after that prefix is `:softland.store0` |

The fifth line is this plan's own reading of the facts table and needs no change of it. The run can
also hold a permission, when the layer's id is also a holder's id; `layers` reads each row by its key,
so one row more changes nothing.

## Depots

None.

## Topologies and PStates

None. The template's rule: "Declare a topology only when work requires it."

## Query Topologies

None. The three of `module/door_reads.clj` stay the door's; the read path calls none of them.

## The form of a query

Declared as data in `softland.store.read-path.query`, in the language of `act.clj` (`shapes`) and with
rules as `door/declaration.clj` has them (`rules`, with the code of each in `rule-checks`). The check
reads both and holds no knowledge of the form itself.

```clojure
{:asks             [:place entity key]            ; a place
                 | [:entity entity]               ; an entity: every place of it
                 | [:run from-end to-end]         ; any run of neighbouring places
                 | [:points-at key thing]         ; what points at a thing
 :asker            id                             ; who asks: exactly one of these two
 :not-signed-in    true
 :layers           [layer ...]                    ; through which layers, nearest first: exactly one of
 :from-layer       layer                          ;   these two. The second takes the list from the layer
 :with             #{:writer :permission}         ; which parts of a row she asks for; left out: none
 :stores-own-facts true}                          ; left out: the store's own facts are not given
```

**What a caller may name as a run (ruling 29).** A run is every place of one layer from one end to the
other, both ends in the run, in the order in which the table keeps its keys: the printed text of the
entity, then the printed text of the key. An end is one of:

| An end | As the first end | As the last end |
|---|---|---|
| `[entity]` | from the entity's first place | to the entity's last place |
| `[entity key]` | from that place | to that place |
| `:first` | from the layer's first place | — |
| `:last` | — | to the layer's last place |

So a caller can name: some keys of one entity (`[:run [e :a] [e :m]]`); one entity, which `[:entity e]`
says shorter; the entities between two ids (`[:run [e1] [e2]]`); from an entity on (`[:run [e] :last]`);
a whole layer (`[:run :first :last]`). Each `entity` is an id and each `key` a key, within the limit of
256 characters. The same two ends are read in each layer of the list; only the layer's part of the
row's key differs (implied spec B3). A run does not leave its layer: `rows/run-of-ends` (function 4)
gives two texts that both begin with the layer's text and a space, the first not after the second. A
tool chose none of these orders: two entities are neighbours only if their ids print as neighbours.
The read path gives the places of a run as a map and states no order of its own.

**`:points-at`.** `key` is a plain key. `thing` is a value as a maker gives it: for a thing whose id is
a UUID, the UUID itself (hand-off). By ruling 10 it can be any value of the form; nil is none, since a
fact with no value has no row in that order.

**Who asks (ruling 31).** `:asker` holds an id. A person who is not signed in is said with
`:not-signed-in true` and no `:asker`: every keyword that reads back is an id, so no keyword can be the
word for her. A query with neither, or with both, is turned away. `:system` is a who like any other.

**The one form** that the check gives: each part there; `:asker` nil for a person who is not signed
in; `:layers` or `:from-layer` nil; `:with` a set; `:stores-own-facts` true or false.

**The shapes** (added to the entries of `act/shapes`, as the door's declaration adds its own):

```clojure
:query        [:parts
               [:asks             :value         {:absent :asks-no-word-known :reason :asks-no-word-known}]
               [:asker            :id            {:optional true :reason :asker-is-no-id}]
               [:not-signed-in    [:enum true]   {:optional true}]
               [:layers           [:seq-of :id]  {:optional true :reason :layers-not-a-list-of-layers}]
               [:from-layer       :id            {:optional true :reason :from-layer-is-no-layer}]
               [:with             [:set-of [:enum :writer :permission] {:max 2}]
                                                 {:optional true :default #{} :reason :part-of-a-row-not-known}]
               [:stores-own-facts [:enum true false] {:optional true :default false}]]
:run-end      [:or [:enum :first :last] [:tuple :id] [:tuple :id :key]]
```

The shape holds `:asks` only to being there. The check then reads its first slot, looks the word up
in `words`, and holds the whole of `:asks` to that word's own shape with `act/as-shape`, so that a word
that is not known and a known word with a wrong part get two different words. An `:or` of the four
shapes would give one word for both. `words` is the declaration of what can be asked:

```clojure
(def words
  {:place     {:shape [:tuple [:enum :place] :id :key]            :reason :place-not-named}
   :entity    {:shape [:tuple [:enum :entity] :id]                :reason :entity-not-named}
   :run       {:shape [:tuple [:enum :run] :run-end :run-end]     :reason :run-not-named}
   :points-at {:shape [:tuple [:enum :points-at] :key :value]     :reason :points-at-not-named}})
```

**The rules**, in the order in which they are examined, each with its word:

| Rule | Word | What it holds |
|---|---|---|
| `:names-one-asker` | `:no-one-asker` | exactly one of `:asker` and `:not-signed-in` |
| `:names-its-layers-one-way` | `:layers-not-named-one-way` | exactly one of `:layers` and `:from-layer`; a list holds one layer at least |
| `:layers-within-the-limit` | `:too-many-layers` | a list holds 65 layers at most (`max-layers-of-one-read`) |
| `:ids-within-the-limit` | `:id-over-the-limit` | the asker, each layer and each entity pass `declaration/usable-id?`; each key is a readable keyword within the limit (`act/readable-keyword?`, `declaration/id-within-the-limit?`), as the hand-off asks |
| `:run-ends-in-order` | `:run-ends-out-of-order` | the first end is `:first` or not after the last end, by `rows/before?`; `:last` is no first end and `:first` no last end |
| `:points-at-names-a-thing` | `:points-at-not-named` | the thing is not nil |
| `:store-key-is-asked-for` | `:store-key-not-asked-for` | a `:place` or a `:points-at` under a key for which `declaration/store-key?` holds needs `:stores-own-facts true`; a `:points-at` under one is turned away either way, since no layer can mark it (ruling 40) |

Two words name no part: `:not-a-query` (the thing is no plain map, or it threw while it was read) and
`:part-not-known` (a part that the form does not hold). `query/reasons` is read from the declaration,
as `act/reasons` is.

**The check.** `(query/check x)` → `{:ok query}` in the one form, or `{:refuse word :at part}`. It never
throws, whatever `x` is. How far that reaches, as the door says of `send-act`: a part that has no end
does not come back. The count of `:layers` is taken with a bound before any layer of it is read, so a
list of ten thousand layers is turned away after 66. The thing of a `:points-at` is held to
`act/value?` and to no size: a thing that is larger than any value that a gate keeps passes, the read
makes its digest and one call, and finds no row. It is the test that the hand-off asks for before any
key is made: after it, each id that a caller gave is an id within the limit, each key a readable
keyword, and the thing of a
`:points-at` passes `act/value?`. An id that comes from a row and not from a caller (a layer of a
list's fact, the entity of a row of the facts by value) is tested with `declaration/usable-id?` where
it is read, and left out when it fails: `layers/builds-on`, `sources/nearer-checks`.

**Nobody decides what a tool asks; a new word is added once, in code.** In three maps that are keyed by
the word, and a check of the pure proof holds that the three have the same keys: `query/words` (its
shape and its word), `sources/of-word` (what it reads in one layer), `answer/of-word` (how its rows
are put together). Finding by text, looking back, what stands on a fact and everything one person
wrote (the sheet's W11, 4 to 7) would each be one entry in the three, and for most a table that the
gates do not keep yet.

## The calls

The read path is `softland.store.read-path`, the file `src-store/softland/store/read_path.clj`, beside
`door.clj` and `module.clj`; its pure parts are five namespaces in `src-store/softland/store/read_path/`.
The door's place in the tree is the precedent: `door.clj` above `door/`. It is the one file of the read
path that requires Rama, and the one namespace of the tree that opens a table to read a fact (R1).

Each call but `open` takes the read path first.

```
open        a cluster handle, the module's name, and when the opener wants, a map of the read
            path's numbers -> a read path, or a word
read-once   a query -> an answer, or a word
read-live   a query, and a function of one argument -> {:now an answer, :leave-with a handle},
            or a word. The function is called with each change that comes after `:now` was made
leave       a handle that `read-live` gave -> :left, or :not-open
open-count  nothing more -> {:live-reads n :pushes n}: how many live reads are open, and how
            many pushes they hold between them
```

**`open`.** Four fetches of a client, once for a process and on no read's way:
`(rama/foreign-pstate cluster module-name "$$streaming-facts")`, and the same for `"$$microbatch-facts"`,
`"$$streaming-facts-by-value"` and `"$$microbatch-facts-by-value"`. It gives the word `:find-the-module`
when the cluster gave one of them not; no read path is then opened. The read path borrows the four
handles and uses them from every caller's thread; the opener owns the cluster handle, and nothing
closes the read path. It prints as `#<read-path module-name>`, by a type of its own, as the door does.
A process opens one read path: two read paths share no push, so B5 would not hold between them, and
every push would be open twice. The read path and the door are opened apart; neither knows the other.

**`read-once`.** It checks the query, reads what `sources/reading` says is missing until nothing is,
and gives the answer. It holds nothing when it returns. Where a push for a source is already open and
given, it takes the value that the push holds and makes no call for that source (B5's "with no read of the facts
from Rama", taken for a read asked once too).

**`read-live`.** The same reading, with a push in place of each select. It gives the answer now, and a
handle. Afterwards it calls the function with one map at each change:

```clojure
{:changed {place place-answer}}   ; the places that this change names, each with what the read gives now
{:anew answer}                    ; a whole new answer, in place of the one before
{:ended word}                     ; the read ended; the function is not called again
```

- `:changed` comes for a fact that changed in a layer of the read. It holds the places that the change
  names, each with what the read gives there now; a place at which a nearer layer holds a row is not
  among them. A place that the read gave and no longer gives holds `{:holds :no-fact}`. When two
  changes cross, a place can come a second time with the answer that it already had; it never comes
  with an answer that is not the newest.
- `:anew` comes when Rama sends a value anew and it differs (ruling 24), and when what shapes the read
  changed: a layer joined or left, the list of the start layer was written again, a key was marked or
  its mark taken off. The whole answer holds each place that a layer had won with what wins it now,
  and says which layers are left out.
- `:ended` comes when the read path can no longer keep the read right: Rama ended a push, a run grew
  past the build's number of places or the checks past theirs, a read that the open read came to need
  failed, or the function itself threw. The caller asks again; the read path opens nothing again by
  itself (ruling 24). A throw of the function while it is given `{:ended …}` is dropped.

**On which thread the function is called, and what it must not do.** On a thread of Rama's client: the
thread of the push whose change it is (T5), or the thread on which a push that the read came to need
was given (run: C3, a function chained to a push's future ran on that push's thread). For an
`{:ended …}` after a wait that passed, on the thread of the JVM's own timer. Never on the thread that
called `read-live`. The read path holds the read's own lock while it calls, so the calls for one read
come one at a time and in the order in which they were worked out.

The function must return soon, and the docstring says what it stops while it does not. Run, C4, on an
in-process cluster whose client had two such threads: while one callback stayed for 2.5 s, every
push whose callbacks run on that thread waited, three of the seven others, pushes of other askers
among them. A select and the opening of a push from any other thread waited as well when Rama's
client gave their answer on that thread: in one run six of six selects that block, four of six that
do not, and four of six openings; in another six, six and three. So one slow function holds back the
reads of others, `read-once` and `read-live`, for as long as it stays, up to their own bound. And a
second change for the same read, on the other thread, waits for the read's lock and so holds that
thread too. A caller that does more than hand the change on takes a thread of its own.

The function may call `leave`, for its own read or for another. `leave` takes no lock and waits for
nothing, and closing a push inside its own callback, or inside the callback of another, returned at
once and ended that push (run: C5, C6). It must not call `read-once` or `read-live`: they wait for
Rama on a thread that Rama may need for the answer. Of six selects made inside a callback, three
were given only after the callback returned; a select that blocks, made inside a callback, came back
in a millisecond in two runs and did not come back in a third (run: C5, C9, and run 1). So the read
path turns such a call away at once, with the word `:read-inside-an-askers-function` and no call to
Rama: `tell!` marks its thread for the length of the call, and `read-once` and `read-live` look at
the mark first. What the function does with a change, and on which thread of its own, is the
caller's.

In the code and its explanations this lock is called the read's **turn**, and never a lock: in this
tree a lock is an encryption key (`src-store/CLAUDE.md`, "Words"), and the read path reads none. This
paper says "the read's lock", as ruling 67 does.

**R5, where a caller meets it.** The function is called with the newest state of the places that
changed. It is not called once for each act: two acts on one place can come as one change, Rama can
leave a reader out of a change and send the value anew, and a second attempt of a batch that writes
the same row gives nothing (ruling 17 (c)). A caller that must see every act cannot rest on this call.
The docstring of `read-live` says so.

**`leave`.** It takes the handle, takes the asker out of each push that her read holds, and closes each
push that no read holds any more. It gives `:left`, or `:not-open` for a read that was left or ended
before, and for a thing that is no handle of this read path. It takes no lock and waits for nothing.
So a call of the function that another thread had begun to make can still arrive in the moment in which
`leave` returns; none is begun after that. Nothing bounds a caller that never leaves but the end of
the process (ruling 25), as nothing bounds the door's stocks.

**A word in place of a throw.** No call throws for what a caller gave or for what Rama did. A word is
a map that holds `:could-not`, one of the read path's own seven (`softland.store.read-path.words`). No
answer holds `:could-not`.

| Word | From | What it says | It carries |
|---|---|---|---|
| `:find-the-module` | `open` | the cluster gave no table of the module under that name | `:module`, `:threw`, `:said` |
| `:read-the-query` | `read-once`, `read-live` | the thing is no query of the form; no call was made to Rama | `:why`, the check's word; `:at`, the part |
| `:reach-the-store` | `read-once`, `read-live` | a call of Rama's client threw; a read that failed has not said "no fact" | `:threw`, `:said` |
| `:get-an-answer-within-its-wait` | `read-once`, `read-live` | the read path's wait, one bound for the whole call, passed before Rama gave every value, or the read did not settle in 8 rounds | `:waited-ms` |
| `:read-inside-an-askers-function` | `read-once`, `read-live` | the call was made on a thread on which the read path is calling an asker's function; no call was made to Rama | — |
| `:serve-a-read-that-large` | `read-once`, `read-live` | a run of a layer that she may see holds more places than one read may, or the read needs more checks of nearer layers than one read may | `:what`, `:most` |
| `:keep-the-read-live` | the function, under `:ended` | the read ended | `:because`: `:the-store-ended-a-push`, `:a-run-grew-too-large` (with `:what` and `:most`: the places of a run, or the checks of the read), `:a-read-it-came-to-need-failed` or `:its-function-threw`; and what that carries |

Every call of Rama's client goes through one function, `attempt`, as in the door, and every wait for a
value of Rama's has the read path's own bound, `:wait-ms`. So a read in the second after a worker of
Rama restarts, when reads from outside throw (the batch probe, "Seen on the way"), ends in
`:reach-the-store` and is asked again by its caller. The read path tries nothing a second time by
itself.

## What a read gives

An answer is a plain map. For every word of a query it holds:

```clojure
{:layers   [layer ...]                       ; the layers that the read took, nearest first
 :left-out [{:layer layer :why :may-not-see} ; each layer of those that gave nothing, and why:
            {:layer layer :why :not-made}]   ;   she may not see it, or it is not made (ruling 8)
 ...}
```

and then, by the word:

| The word | The answer also holds |
|---|---|
| `:place` | `:place`, one place-answer |
| `:entity`, `:run` | `:places`, a map from `[entity key]` to a place-answer, for each place of the run at which a layer that she may see holds a fact |
| `:points-at` | `:places`, the same, for each place that points at the thing; and `:orders`, for each layer that she may see, `{:layer layer :level-since stamp}` or `{:layer layer :not-marked true}` |

**A place-answer** tells the four things of ruling 14 apart, and says which layer won:

```clojure
{:holds :no-fact}                                   ; no layer that she may see holds a fact there
{:holds :nothing-here  :layer l :fact f :stamp s}   ; the nearest fact there says "nothing here"
{:holds :value :value v :layer l :fact f :stamp s}  ; the nearest fact, with its value as its maker gave it
{:holds :not-a-value   :layer l :fact f :stamp s}   ; the nearest fact's bytes are no value of the form
```

`:layer` is the layer that won the place. `:fact` is the fact id `[[layer depot by-act] by-index]`,
which a writer names in `:replaces`; the depot is `first-act/depot-at-making` of the layer (ruling 30).
`:stamp` is the stamp of that fact's decision, which a writer carries in `:based-on`. The four words
are `rows/read-value`'s own. With `:writer` in the query's `:with`, the place-answer also holds
`:writer`, the id that `rows/who-of` gives; with `:permission`, it holds `:permission`, what
`rows/permission-of` gives. A read that asked for neither is handed neither (R4, ruling 15): one
function, `answer/place-answer`, makes every place-answer, and it puts a part in only when the query
asked. Rama sends the runtime the whole row either way; the docstring says what crosses.

**Which layer wins.** For each place, the nearest layer of `:layers` that she may see and that holds a
row there, also when that row says "nothing here" (B3). A layer that is left out wins nothing.

**The store's own facts (ruling 11).** A row whose key is one of the store's own
(`declaration/store-key?` of the key that `rows/place-of-key` gives, as the hand-off says) counts as no
row unless the query holds `:stores-own-facts true`. One function says it, `answer/counts?`. So the run
of the entity `:alice` does not give her permission, and a run across entities gives no request for
locks.

**What points at a thing, through layers (rulings 12, 20, 41, 43, 64).**

- A layer of the list marks the key or it does not. If it does, with the stamp S, the answer says
  `{:layer l :level-since S}`: that layer's order holds every fact under the key whose stamp is later
  than S, and no other (the part before's one sentence for both gates). If it does not, the answer
  says `{:layer l :not-marked true}`, and that layer's order gives no place. The read never reads a
  layer whole for it.
- A place is found through an order: the read looks at a place when the order of some layer that she
  may see holds it. At such a place the nearest layer that she may see and that holds a counted row
  wins, as for every word (B3). A layer holds a row there when its own order holds the place, or when
  the check of ruling 12 finds a row in its facts table. The check reads the facts table of each
  layer that is nearer than the nearest order which holds the place, marked or not.
- The place is given when the winner's value is the thing that was asked for (ruling 64). When the
  winner's own order holds the place, it is given from that order's row. When the winner holds the
  place by its check, it is given from the check's row if `rows/holds-this-value?` says that the
  row's value is the thing, and it is not given if the row says "nothing here" or holds another
  value. So a nearer fact that moves the block, or takes it away, hides it. A nearer fact that says
  the same thing, written before that layer's mark or in a layer that does not mark the key, does
  not hide it: the place comes from the nearer layer, with that layer's `:layer`, `:fact` and
  `:stamp`. It costs one digest of the row's bytes for each check that finds a row.
- What an asker must know of this, and the docstring says it. A place can come from a layer whose
  entry in `:orders` says `:not-marked`, or with a stamp that is not later than that layer's
  `:level-since`. And a place is found only through an order: a place at which a nearer layer says
  the thing, and which no order of a layer that she may see holds, is not in the answer;
  `:level-since` and `:not-marked` say for each layer what its order leaves out. So when the farther
  layer's fact at such a place moves away, the place leaves the answer, though the nearer fact still
  says the thing.

**One moment for all the layers (ruling 13).** None. The layers are read at moments that differ, and
in a microbatch layer two tasks show one batch at two instants. Nothing lines them up.

## The pure core

Four functions, with no cluster, in three namespaces. The code that calls Rama holds no rule of a read:
it fetches what the third function says is missing, and hands the fourth what it fetched.

`known` is a map from a source to the value that a select gave or that a push holds now: a row or nil
for a source of one row, a sorted map from key to row for a run. A source that `known` does not hold
is not read yet; a source that it holds with nil is read, and there is no row.

**(1) Which layers, in what order: `layers/in-order` (ruling 9; the one function).**

- `:layers [..]`: the list, each layer once, at its first place.
- `:from-layer s`: `s`, then the layers of `s`'s builds-on fact in the fact's order, each once, at its
  first place, `s` itself never a second time. No further: the lists of those layers are not read. With
  no fact the list is `s` alone (`[]`, hand-off).
- While `s`'s own rows are not known, it gives nil, and `reading` takes `[s]` for what is wanted in
  that round; no answer is made from it. When she may not see `s`, or `s` is not made, it gives
  `[s]`: the list is a fact of a layer that she may not see, so the read does not use it (open
  point 6).

**(2) Is a layer made, and may this asker see it: `layers/standing` (rulings 30, 31; entry 1).**
From the layer's own rows and the asker's permission row:

| The layer's own rows | Its setting | Her permission row | It gives |
|---|---|---|---|
| not known yet | | | `:not-known-yet` |
| no depot row | | | `:not-made` |
| a depot row | `:everyone` | any, or not read | `:yes` |
| a depot row | `:members` | she is not signed in | `:may-not-see` |
| a depot row | `:members` | not known yet | `:not-known-yet` |
| a depot row | `:members` | a row | `:yes`: any permission that is there allows seeing |
| a depot row | `:members` | no row | `:may-not-see` |

The setting is `rows/read-value` of the row under `:softland.store/who-may-see`; with no row it is
`(first-act/starts-with layer :softland.store/who-may-see)`, the kept start of ruling 6; a row whose
value is neither word reads as `:members`. The record is the one that `first-act/depot-at-making`
names for the layer (ruling 30). Ruling 16 (3): these rows are read straight in the base, as a tool's
facts are.

**(3) Which sources are wanted now: `sources/reading`.** It takes the query, `known`, the routing ids
of the tasks, and whether the read is asked once or kept live. It gives the layers, each layer's standing,
the sources that are wanted, and those of them that `known` does not hold. A source is wanted by
these rules and no other:

| Rank | The source | Wanted when |
|---|---|---|
| 0 | S1 of the start layer | the query names `:from-layer` |
| 1 | S1 of each layer of the list | the list is known |
| 2 | S2, her permission in a layer | she is signed in, and: once, as soon as the layer is in the list; live, when the layer is known to be made and its setting to be `:members` |
| 3 | S3, S4: the place or the run in a layer | once: as soon as the layer is in the list. Live: when the layer's standing is `:yes`; a source that `known` already holds stays wanted while the standing is `:not-known-yet`, so that a push is not closed and opened again while her permission is read |
| 3 | S5: the run of the facts by value in a layer | the layer's own rows are known and mark the key, and its standing is `:yes`. Asked once, the standing is known by then: her permission came in the call that brought the own rows |
| 4 | S6: a check in a nearer layer | `sources/nearer-checks`, below |

A layer whose standing is `:may-not-see` wants nothing but its S1 and her S2: those are what could
let it in later (ruling 23). A layer that is `:not-made` wants its S1 alone: a layer that is not made
holds no permission, and its making shows in its own rows.

The start layer of `:from-layer` is in the list from the first round, before its own rows are known:
its S2 and its fact source are wanted with its S1, as for a layer of a given list. So a place from a
layer whose list names two more is one call, and then two.

*Why a read asked once reads a layer's facts before it knows that she may see the layer.* The rights
and the fact then go in one call for a layer (S1, S2 and S3 as one `multi-path`), so a place through k
layers is k round trips in one round, where rights first would be 2k in two. What is read early is
bounded: one row, or the first 256 places of a run. What is given is not changed by it: `answer` gives
nothing of a layer whose standing is not `:yes`. A live read opens no push early, because an open push
costs the layer's gate at each write. One predicate holds the rule, `sources/before-standing?` (open
point 2).

*Ruling 12's check: `sources/nearer-checks` (the one function).* For each place that the order of
some layer of the list gives: one source S6 in each layer of standing `:yes` that is nearer than the
nearest layer whose order holds that place. A layer behind that one cannot win the place, so nothing
is read there. It is the only place where a source depends on the rows of a run. Left out, it gives
the cheap answer, the runs joined: `answer` then finds no nearer row and shows the place from its
nearest order.

*The loop ends, and its rounds are bounded.* A round fetches every source that is missing and asks
`reading` again. A source of rank r is wanted as a function of the query and of the values of sources
of ranks below r, as the table shows: rank 1 needs the list, rank 2 a setting, rank 3 the marks and
the standing, rank 4 the runs of rank 3. No source needs a source of its own rank or above. So after
round i every wanted source of a rank below i is known, and after five rounds nothing is missing: five
ranks. By the query: a place through a given list, asked once, is 1 round; from a layer, 2; kept live
through layers that are "members", 3 (own rows, permissions, places); what points at a thing, asked
once from a layer, 4; kept live, 5. A write that changes a list or a setting while a live read opens
can make one round be made again; the code stops at 8 rounds and gives
`:get-an-answer-within-its-wait`.

Inside the round that fetches a run, a run of more than 256 places costs two calls more, one after
the other (section "R3, and a very large run"). They are steps of fetching one source, not rounds of
the core.

**(4) What is given: `answer/whole` and `answer/of-places`.** Both take the query, what `reading`
gave, and `known`. `whole` gives the answer of the section above. `of-places` gives the place-answers
of a set of places, or nil for a place that still waits. Each works out a place by one walk from the
nearest layer, over the rows that `known` holds at that moment for the sources that `reading` wants.
For a place or a run: the first layer in order, of standing `:yes`, whose value holds a counted row at
the place's key. For what points at a thing: the first such layer that holds any row at the place, by
its own order or by its check; the place is then given when that layer's order has it, or when the
check's row holds the thing (ruling 64). The walk stops at the layer that wins. A place waits when
the walk meets, before it stops, a layer whose standing is `:not-known-yet`, or a wanted source that
`known` does not hold; a source behind the layer that wins is not waited for. Neither function keeps
anything, and neither is handed anything that was put together before (ruling 26, R2).

## Live

**What one live read opens.** One push for each wanted source, each with a partitioning key and a
callback: `(rama/foreign-proxy-async [path] pstate {:pkey key :callback-fn f})`. A push is opened on two
path forms only, the two that the push probe ran on a table of the facts table's shape (its F1 to F3,
Z1 to Z3): `(keypath key)` for one row, and `(sorted-map-range from to)` for a run. Never on a whole
table, and never on a collection that keeps its elements apart: the store has none below a table's top
level.

The worked case, for the report: `:alice` keeps the place `[:note-41 :text]` live through
`[:alice-notes :team :base]`; the first two are streaming and "members", the base is microbatch and
"everyone".

| # | Table | Partitioning key | Path | Shared by |
|---|---|---|---|---|
| 1 | `$$streaming-facts` | `:alice-notes` | `(sorted-map-range ":alice-notes :alice-notes :softland.store/" ":alice-notes :alice-notes :softland.store0")` | every read through `:alice-notes` |
| 2 | `$$streaming-facts` | `:team` | the same run of `:team` | every read through `:team` |
| 3 | `$$microbatch-facts` | `:base` | the same run of `:base` | every read through the base |
| 4 | `$$streaming-facts` | `:alice-notes` | `(keypath ":alice-notes :alice :softland.store/permission")` | her reads through `:alice-notes` |
| 5 | `$$streaming-facts` | `:team` | `(keypath ":team :alice :softland.store/permission")` | her reads through `:team` |
| 6 | `$$streaming-facts` | `:alice-notes` | `(keypath ":alice-notes :note-41 :text")` | everyone who looks at that place in that layer |
| 7 | `$$streaming-facts` | `:team` | `(keypath ":team :note-41 :text")` | the same |
| 8 | `$$microbatch-facts` | `:note-41` | `(keypath ":base :note-41 :text")` | the same |

Eight pushes: three for the layers' own rows, two for her permissions (none for the base, whose setting
is "everyone"), three for the place. It is the implied spec's count, "3, and up to 3, and 3", with the
setting, the depot, the list and the marks of a layer on one push. For an entity or a run, lines 6 to 8
are run pushes; a run across entities of the base is one push for each task. For what points at a
thing, lines 6 to 8 are pushes on the facts by value, for the layers that mark the key, and there is
one push more for each check of ruling 12.

**What a callback is.** Rama calls `(f new-value diff old-value)` on a thread of its client, the
changes of one push in order and the changes of two pushes in no order (T5; implied spec H8). The read
path handles one callback in these steps.

1. *What kind.* A `DestroyedDiff` is the read path's own close: nothing. An
   `UngracefulTerminationDiff` is Rama ending the push: each read on it ends with
   `:the-store-ended-a-push`. The first callback of a push, a `ResyncDiff` with no old value, is the
   value that the opening already gave: nothing (T5; one mark for each push says that it came).
2. *Which keys the change names: `sources/named`.* For a push on one row, its key. For a run, the keys
   that a processor of `KeyDiff` and `KeyRemoveDiff` is handed; Rama expands a `MultiDiff` into
   those (run: T5), and a `KeysDiff` and a `KeysRemoveDiff` too (read, not run: `15-pstates.md:706`). For a later `ResyncDiff`, and for any diff that
   the processor does not handle, every key of the old and the new value.
3. *Which of them changed.* A key whose row is the same before and after is dropped:
   `rows/same-row?` of the old value's row and the new value's row, field by field, the value's bytes
   by their content. This one test is ruling 17 (c), a push that calls back with a value that is the
   same; it is the row written again by a second attempt of a batch (batch probe B7.3, S5 and S7); and
   it is the hand-off's removal of a place that the run never held, where both rows are none. When no
   key is left, the callback ends here.
4. *Too large.* A run whose new value holds more places than `max-places-of-one-run` ends each read
   on it with `:a-run-grew-too-large`.
5. *For each read that holds the push*, under that read's lock. A read that is not opened yet does
   nothing here: `read-live` reads the pushes' values itself under the same lock ("How a live read
   opens", below). For an opened read one pure function says what the step does,
   `answer/after-change`, and the code of `read_path.clj` only does it. In this order:
   - `sources/reading` again, on the values that the read's pushes hold now. A source that is now
     wanted and not held is joined; one that is held and no longer wanted is left. Then `known` is
     made again, of the values of the sources that the read holds after that and that are given. A
     source that this step left is not in it. A source that this step joined is in it only when
     its push was open already, for another read.
   - *Is the whole answer owed?* It is owed from this step on in three cases. The source is a
     layer's own rows or her permission, and what it means for this query differs before and after
     (`layers/meaning`, below). A whole value came anew and differs. Or this step joined or left a
     source that is no check of ruling 12, whatever source called: such a source is joined or left
     only when what shapes the read has changed in the pushes' values, and the step that sees that
     first need not be the step of the push that changed. It can be owed from an earlier step: the
     read holds one mark for it. While it is owed, no single place is told.
     When nothing is missing, the function gets `{:anew (answer/whole …)}` and the mark is taken
     off. When something is missing, the mark stays, and the step in which the last missing source
     is given gives the whole answer.
   - *Else the source is a fact source.* For each place that is left from step 3, `answer/told-by?`
     says whether this change is to be told: the source's layer has the standing `:yes` for this
     asker, the row counts (ruling 11), and no nearer layer of standing `:yes` holds a counted row
     at the place in `known`: in its run, in its order or by its check. Then the place is worked out
     with `answer/of-places`; a place that waits is left out. What is left goes to the function as
     `{:changed {place place-answer}}`. A source that this step joined has its arrival handled when
     its push is given (step 6); when the push was open already, that is at once.
6. *A push that an open read came to need is given.* Under the read's lock, by `answer/after-arrival`:
   step 5's first line again, which owes the whole answer if it joins or leaves a source that is no
   check. Then, when the whole answer is owed, `{:anew …}` if nothing is missing any more. Else, when
   the source is a check of ruling 12, `{:changed …}` for its place, unless the place still waits. A
   read that no longer holds the source does nothing.

`layers/meaning` gives what a layer's own rows and her permission row mean for this query, as a value
that `=` compares: whether the layer is made; its setting; whether her permission row is there; and,
only when her standing with these rows is `:yes`, the list when it is the start layer, and the stamp
of the asked key's mark. Step 5 calls it with the old value of the push that called and with the new
one, each beside what `known` holds of the other. So a setting that is written again with the same
word gives nothing. And a list or a mark that is written in a layer that she may not see gives her
nothing: not even a whole answer that says what it said, which would tell her that something of that
layer was written.

Why a step that joins or leaves a source owes the whole answer, and not only the step of the push
that changed. When a setting goes to "members", a fact's step that runs before the setting's own step
already finds the new setting in the push, joins her permission, finds her standing not known yet,
and tells nothing of its place. If she holds a permission, her standing is `:yes` before and after.
The fact's step owes: the whole answer comes when her permission is given, with the fact in it. The
setting's own step then finds that the setting differs and gives the whole answer once more. So no
step that tells nothing rests on a later step of another push.

**How a live read opens, and what its asker gets first.** `read-live` makes the read's entry, not
opened, and goes round: `reading`, join what is missing, wait for the joined pushes, again. One bound
holds for the whole call, `:wait-ms`. It holds no lock while it waits: a callback that waited for the
read's lock would hold a thread of Rama's client, and the opening of a push can need that thread
(run: C4 and C5 of the check's probe). When nothing is missing it takes the read's lock, reads the
pushes' values and asks `reading` once more. If a change that came meanwhile made a source wanted, it
lets the lock go and goes round again. Else it makes `:now` with `answer/whole`, marks the read
opened, lets the lock go and returns.

A callback that comes while the read opens takes the read's lock, sees that the read is not opened,
and does nothing. Its change is not lost. A push holds its new value before its callback runs (run:
C1), and `read-live` reads the values under the same lock. So either the callback had the lock first,
and `:now` holds the change; or `read-live` had it first, and the callback then finds the read opened
and tells the change. An asker gets `:now` and then every later change. A change that fell in the
opening can come once more after `:now`, with the answer that `:now` already holds. None comes that is
older than `:now`.

One thing the read path cannot order. The function can be called, on Rama's thread, before
`read-live` has handed `:now` to its caller's thread: the lock is let go before the call returns. A
caller that keeps a screen puts `:now` in place before it applies a change; its function may wait
for that. The docstring of `read-live` says so.

**Why no put-together value is kept, and why the asker still ends at the newest state.** A step works
from the values that the pushes hold when it runs, and from the old and the new value of the one push
that called. It does not compare with what the asker was told before, because the read path does not
keep that. A push holds its new value when its callback runs, and another push of the same row can
still hold the old one at that moment (run: C1, C2). So two steps that cross each see the other's
change or do not, and each test that lets a step say nothing must be safe either way.

Three tests let a step say nothing of a place. Each is true whatever the push that called holds.

- *The row is the same* (step 3).
- *A layer's own rows mean the same* (`layers/meaning`).
- *A nearer layer that she may see holds a row at the place now*, in a source that the read still
  holds after this step. Then the answer at the place does not rest on the layer that called: the
  walk stops before it. And the nearer source's own change is told by its own step, now or later,
  because the read still holds it. A row of the facts table never goes away. An order that loses the
  place calls back. And a check is left only by a step after which the answer no longer rests on it:
  no order holds the place any more, and that step tells the place (next line); or a nearer layer's
  order has come to hold the place, and that layer wins before the check's layer is reached.

Three things are told with no such test, because a test there could hide a change for good.

- *A place whose sources this step changed.* When an order loses a place and no order holds it any
  more, the step leaves the checks at that place. A change that such a check's push has taken and not
  yet called back then never reaches the read. So the step counts only sources that it still holds,
  finds no nearer row, and tells the place: not in the answer. When an order comes to hold a place,
  the checks nearer than it are joined, and the place is told when the last of them is given.
- *A check that arrives* tells its place (step 6).
- *What shapes the read* gives the whole answer anew, from every value that the pushes hold then: when
  the push that changed calls back with another meaning, and when any step finds the change first and
  joins or leaves a source for it. The mark "the whole answer is owed" carries that from the step
  that found the change to the step in which the last missing source is given. It is the one thing
  that a read holds between two steps about what its asker is told. It is no value of the tables: it
  is true or false.

Why the last thing that is told at a place is then the answer at the end. Take a place, and the last
step that told it, with the values that it used and the layer at which its walk stopped. A later step
of a source at that layer, or nearer, cannot have said nothing: the sources nearer than it hold what
they held, so no nearer layer holds a row at the place, and a source that was joined since is told
when it arrives. So every later step at that place is of a source behind the layer that won, or of a
source that the read had left, and the answer does not rest on those. The one way in which it could
rest on a farther source is ruling 64's: a nearer check's row that holds the thing counts only while
some order holds the place. That ends with the step that leaves the check, which tells. The read's
lock puts the calls of one asker's function in the order in which they were worked out.

Derived, and played, and not run on Rama. The check's model
(`plan-check-probes-read-path/model/crossing.py`, its log beside it) plays these steps for what
points at a thing in every order in which the pushes can take their values, call back, be served and
arrive: 13 scenarios through two, three and four layers, and every list of up to three writes on one
place in two layers, 672 lists and 22,906 states. With the steps as above, the asker's screen at the
end equals what a read asked once gives, in every order. With `told-by?` counting a check that the
same step has just left, 118 of the 672 lists have an order that ends wrong: the asker keeps a place
that the tables no longer give. With `{:anew}` given by the kind of the source that arrived, and no
mark, a layer that joins over a farther order never gives its own places. With a place waiting for
every wanted source, and not only for those before the layer that wins, a case of four layers ends
wrong. A comparison of "before" and "after" with the other pushes as they are now is wrong too: when
a nearer layer gets a row at a place in the same moment in which a farther order loses that place,
each change alone looks like no change. A second model (`model/standing.py`) plays one place through
two layers of which the nearer has a setting and, for the asker, a permission or none: 620 lists of
up to three writes of the setting, the permission and the two facts. It ends right in every order
with the rule of step 5. It ends wrong for 92 lists when a step of the setting owes the whole answer
only if the asker's standing differs: a fact's step that saw the new setting first told nothing, and
nothing told it later. The build's pure proof plays the same orders on the real functions (check
P9).

The cost of telling without a test: a place can be told a second time with the answer that it
already had.

**What a live read follows beside the facts (ruling 23), and what each change does.**

| What changes | Its push | What the open read does |
|---|---|---|
| A layer of the list is made | its own rows (S1) | its standing can become `:yes`: its fact sources are joined, and the whole answer comes anew |
| A setting goes to "everyone" | S1 | the same, for an asker who held no permission |
| A setting goes to "members" | S1 | her permission's push is joined. Until it is given, nothing of that layer is told. With a permission: the answer comes anew and is the same but for moments. With none: the layer is left out (below) |
| A setting is written again with the same word | S1 | nothing: the row changed and its meaning did not |
| A permission is given to her in a "members" layer | S2 | the layer joins: its fact sources are joined, and the whole answer comes anew |
| The list of the start layer is written again | S1 of the start layer | layers join and leave by the new list; the whole answer comes anew |
| The asked key is marked in a layer | S1 | the layer's order is joined; the whole answer comes anew, with `:level-since` |
| The mark is taken off | S1 | the layer's order is left; the answer comes anew with `:not-marked` for that layer |
| The mark is put on again | S1 | the key has another stamp, so the order is another run: the old one is left, the new one joined |

A layer of the list that is left out keeps its S1, and her S2 while it is made and its setting is
"members": those are the pushes that say when it may come in.

**The ending of what is no longer rightful (ruling 24).** When a layer's standing for an asker goes
from `:yes` to `:may-not-see`, her read leaves that layer's fact sources at once, in step 5; the pushes
go on for their other askers. From then on `answer` gives nothing of that layer. The function gets
`{:anew answer}`: each place that the layer had won is there with what wins it now, and `:left-out`
holds `{:layer l :why :may-not-see}`. That is the word, and it is all that she is told: nothing of the
layer's facts, not that a place of it changed, not how many places it holds. A fact of that layer
whose callback is handled after the setting's is not told, by `told-by?`. One that Rama called back
before the setting's change reached the read path can still be told: "as late as a push is, and no
sooner" (implied spec B4); C measures how late that is. In this build only the setting can end a
right: nothing ends a permission. The room for an ending permission is there: S2 going from a row to
none is the same path.

**Rama ends a push, sends a value anew, or calls back with the same value.**

| What Rama does | Seen as | What the read path does |
|---|---|---|
| Ends a push after too many errors | an `UngracefulTerminationDiff` (read, not run: `15-pstates.md:732`) | each read on it ends: `{:ended {:could-not :keep-the-read-live :because :the-store-ended-a-push}}`. Its other pushes are left. Nothing is opened again |
| Sends a value anew | a `ResyncDiff` after the first (batch probe S7; read: `15-pstates.md:710-720`) | if every row is the same, nothing. Else each read on it gets the whole answer anew |
| Calls back with a value that is the same | a `NewValueDiff` or a `KeyDiff` whose row is the same (T5; batch probe S5) | nothing (ruling 17 (c)) |
| Leaves a reader out of a change (a streaming table under load) | a later `ResyncDiff` (read, not run: `15-pstates.md:1230`) | as "anew": the asker ends at the newest state and may have missed states between (R5) |

**A read that an open read came to need fails.** A push that step 5 joined is not given within the
wait, or its opening throws, or its run is too large: the read cannot be kept right, so it ends with
`:keep-the-read-live` and the reason. No thread waits for such a push: the wait is a bound on its
future (`CompletableFuture.orTimeout`), and when it passes the read is ended on the thread of the
JVM's timer. An opening that throws is caught where it is made, so that the entry's delay holds a
future of a word and never a thrown thing; each read of the entry ends or gives the word, and the
entry leaves the map with its last reader.

## Sharing, and what the read path holds

**What "the same thing" is.** A source: the map of its gate, its table, its routing id (the
partitioning key that Rama's client is given) and its path. Two askers look at the same thing in a layer when their reads want equal sources. The read path
holds one push for one source, however many reads want it. So two askers whose lists differ share the
push of each layer that both lists hold (implied spec B5), and a read of a place and a read of a run
that holds the place are two sources and two pushes: a place is not served from an open run (it could
be; the saving is one push for each such pair, and it would make "the same" a question of one run
holding another).

**Askers who ask for different parts share.** `:with` and `:stores-own-facts` are no part of a source.
The push carries whole rows for all; `answer/place-answer` hands each asker what she asked for
(ruling 15).

**What is held, in the memory of one process.**

One atom, `held`, of a map with two parts, and beside it what is borrowed.

1. `:pushes`: a map from a source to `{:opening … :readers #{read-id} :called …}`. `:opening` is a
   delay around the one call that opens the push, and gives Rama's future of the push. `:readers` are
   the live reads that hold it. `:called` marks that its first callback came.
2. `:reads`: a map from a read's id to its query in the one form, its function, its lock, the sources
   that it holds, whether it is opened, and whether the whole answer is owed ("Live", step 5). A
   source in a read's set is the same object as its key in `:pushes`, so a source is held once however
   many reads hold it. A source of a run across entities in a microbatch layer holds the routing id
   of its task, as it was read when the read opened.
3. The four handles, borrowed, and the numbers.

That is who looks at what, what each asked for, and the pushes (ruling 26). The values are in Rama's
pushes and nowhere else: each `ProxyState` holds the current value of its path, and Rama keeps it
level. Sid's ruling is on that value: "yeah i also don't consider it a cache".

**Why none of it is a store of our own beside Rama's push (R2).** The read path holds no row, no value,
no answer and no place-answer in a field or an atom. `answer` is called with the values that the pushes
hold at the moment of the call, and what it gives goes to the caller and is dropped. A read asked once
holds what it fetched until it returns. The opening of a run holds the count of a bounded read for the
length of that opening. Nothing that the read path holds needs to be kept fresh: an entry of `:pushes`
or `:reads` changes only when an asker comes or leaves, or a read comes to want another source. This
session found no need for a kept value, and so did not stop: the one place where one seemed needed, to
tell "changed for her" from "not changed", is met by the tests of "Live", step 5.

Two held things are nearest to that line, and the check of this plan weighed both. The mark "the
whole answer is owed" is about what the asker is told; it holds no value of the tables and is never
kept fresh: it is set by one step and taken off by another. The set of a read's sources follows the
tables: for what points at a thing it holds one check for each place of a farther order and each
nearer layer. It is made again from the pushes' values at each step, by `reading`, and no answer is
worked out from it: an answer comes from `known`.

**How it is counted.** A read is in `:readers` of each source that it holds, and each such source is
in the read's own set. Joining and leaving are each one `swap!` of `held`, for all the sources of one
step at once, so the two parts never disagree.

- *Join.* For a read that `:reads` still holds: where `:pushes` holds no entry for a source, the swap
  puts one in, with a new delay and the read as its one reader; else it adds the read. A read that
  was left in the meantime joins nothing. After the swap, the read forces each entry's delay.
- *Leave a source, or leave whole.* The swap takes the read out of each entry; an entry with no reader
  left is taken out of the map; `leave` also takes the read out of `:reads`. After the swap, the
  leaver closes the push of each entry that its own swap took out: `close!` when the future is
  given, or when it is given later. It forces the entry's delay to do so. A delay that its joiner
  had not forced yet then opens a push that is closed as soon as it is given: that is the price of
  never leaving a push open that no entry holds. `close!` returns at once and waits for nothing,
  also inside a callback of that push (run: C5, C6).
- *After a leave.* `tell!` looks, under the read's lock and just before it calls, whether `:reads`
  still holds the read; a read that was left is given nothing. A step that reads the value of a push
  which another thread's `leave` closed in that moment gets a throw from Rama's client ("Tried to
  fetch from closed ProxyState", T6); `known-of` catches it, and the step stops for that read.

**The two races of B5.**

- *One joins while the last one leaves.* Both are swaps of the one atom, so one is first. Leave first: the
  entry is gone, the joiner puts a new entry in and opens a new push; she is never handed the push that
  is closing, because a push is closed only after its entry left the map. Join first: the entry has
  two readers, the leaver takes itself out, and nothing is closed.
- *Two open the same at once.* Both swaps end with one entry that holds both readers, and both force
  the same delay. A delay runs its body once, so `foreign-proxy-async` is called once. A swap that is
  tried again makes a delay that nobody forces.

**A second asker of an open push** forces a delay that is done: no call to Rama, the value that the
push holds (implied spec B5). Her standing is still read for her, from the layer's own rows, which are
most often open too, and from her own permission.

**What is lost at a restart of the runtime.** All of it. Each live read is gone with the process; Rama
clears a push that sends no heartbeat (read, not run: `15-pstates.md:728`). Nothing tells a caller;
each asks again. The cluster holds nothing of a read.

**A caller that never leaves.** Its read and its pushes stay until the process ends. `open-count`
shows the count. The docstring says so (ruling 25).

## The base and any microbatch layer: which task each word reads

A layer's gate is `first-act/depot-at-making` of its id (ruling 30). In a streaming layer every row is
on the layer's task, and every source has the layer as its partitioning key. In a microbatch layer:

| The source | Where its rows are (hand-off) | Partitioning key | Tasks read |
|---|---|---|---|
| S1, the layer's own rows; S2, her permission | on every task | live: the layer id, so that every asker shares one push. Once: the key of another source of that layer in the same round, so that they ride its call; else the layer id | 1 |
| S3, a place | the task of its entity | the entity | 1 |
| S4, an entity; a run whose two ends name one entity | the task of that entity | the entity | 1 |
| S4, a run across entities | one part on each task | the key of each task | T |
| S5, what points at a thing | the task of the value's digest | `(rows/value-digest-of V)` | 1 |
| S6, a check in a nearer microbatch layer | the task of the place's entity | the entity | 1 for each place |

**How a client names a task, settled first (T1 to T4, run).** The read path learns the number of tasks
from the handle, `(:num-partitions (rama/foreign-object-info microbatch-facts))`, and takes one key
for each task from Rama's own class, `(com.rpl.rama.Helpers/genHashingIndexKeys n)`: a list of n
Strings whose i-th Rama's hash partitioner sends to task i. A select or a push with `{:pkey (nth keys
i)}` reads or watches task i. All four steps were run on Rama 1.6.0: the count from a table's handle;
the keys, equal to the test namespace's, which the module's proof uses for its own selects; a select
of a run on each task; a push of a run on each task of a table that a microbatch topology writes. The
number costs no call: `foreign-object-info` answers from what the client holds (run: C8, 200 calls in
0.1 ms). A read asked once reads it at each call and remembers nothing, as the door remembers no
layer's depot. A live read with a run across entities reads it when it opens, and its sources hold
the routing ids for as long as it is open. If the number of tasks changed under such a read, it would
go on watching the tasks of before, and nothing would tell its caller: the docstring says that such a
read must be opened again, and the receipt says that nothing notices it. What was not run: this on
the store's own tables (the build's proof does);
a number of tasks that changes under an open read, which is the other sheet's (Z6); and whether Rama
means that class for a client outside a test (open point 3). It can be done soundly as far as one
machine shows, so no case is bent for it.

**A run across entities** in a microbatch layer is T sources, one for each task, each read and sized
by itself. `answer` looks for a place's key in each part. **A row that every task holds is given
once:** such a row is under a store key, so it is in an answer only when the query asks for the store's
own facts; then `answer/one-of-each` takes, for one key that several parts hold, the row with the
latest stamp. The tasks do not show a write of such a row at one instant (hand-off), so two parts can
differ for a moment, and the later one is the newer.

**What a row of a microbatch layer may rest on.** A row that a task shows is the row that every
attempt of its batch makes (`PLAN-base-for-a-reader.md`, "What this part hands on"), if ruling 17 (d)
holds in that part's build. So the read path reads the base straight, as it reads a streaming layer,
and holds nothing back. A second attempt writes the same rows again; a push then calls back with a row
that is the same, and step 3 of "Live" gives nothing. If that build stops the repair and keeps the row
before beside the new one, this plan changes where it reads a row of the base: `answer` and
`sources` would read two rows for a place and the task's `:closed-through`. That plan says what the
other way asks; nothing of it is designed here.

**What a read must not rest on, and does not** (hand-off): that a fact's row and its row of the other
order change together in a microbatch layer; a lock.

## R3, and a very large run

**Every path that the read path gives Rama is one of five forms**, built by one function,
`read-path/path-of`, from a source whose texts `record/rows.clj` made of ids that were tested first:

| Form | Path | Used for |
|---|---|---|
| 1 | `(keypath key)` | one row: a select, a push |
| 2 | `(sorted-map-range from to)` | a run: a select, a push; and the layer's own rows |
| 3 | `[(sorted-map-range-from from {:max-amt 257}) (sorted-map-range from to)]`, and the same with `(view count)` after it; 257 is `:run-start-places` and one | the first read of a run: a select |
| 4 | `[(sorted-map-range-from from {:max-amt 20001}) (multi-path (view count) [LAST FIRST])]`; 20,001 is `:max-places-of-one-run` and one | the size of a run: a select, alone in its call, since it gives one value or two |
| 5 | `(multi-path …)` of forms 1, 2 and 3 | the reads of one task, asked once |

**How the read path itself refuses a watch of a whole table (R3).** No caller gives a path, a table or
a partitioning key: a query is four words. The widest thing that a word can name is `[:run :first
:last]`, the whole of one layer, which is a run and no table (the push probe's Z5): its two ends are
`"<layer> "` and `"<layer>!"`. A source of any other kind makes `path-of` give nil, and the read path
then makes no call. The reason is stronger than the 100 seconds of the push probe: one path form that
Rama does not serve stopped a worker (T9). And an opening that does not come back ends in a word after
`:wait-ms`, since each opening is a future with a bound; the push is closed if it is given later.

**A very large run.** Rama serves a run of any size: 200,000 places opened as one push in 298 ms (U6),
and the client then holds every row. A layer's size has no bound, an entity's keys have none, and one
value can have any number of places that point at it. So the read path bounds each run before it reads
it whole. No exact and bounded form exists for a large run (U1), so there are three steps, and a small
run takes only the first.

1. **The first 256 places, in the layer's own call.** Form 3: a walk of at most 257 rows from the run's
   start, cut at the run's end on the task. Fewer than 257 came back: that is the whole run. Asked
   once, it is the answer's rows. Kept live, the count is asked (form 3 with `(view count)`), and the
   push is then opened on form 2.
2. **257 came back: the size, with a bounded walk.** Form 4 gives the count of the first 20,001 rows
   from the run's start and the key of the last of them. The run holds more than 20,000 places exactly
   when the count is 20,001 and that last key is before the run's end (`sources/over-the-limit?`,
   with `rows/before?`). Then the read gives `:serve-a-read-that-large` with `{:what
   :places-of-one-run :most 20000}`, and no place.
3. **Else the run whole.** Form 2: a select, or the push.

After the push is open, each change is counted (step 4 of "Live"). The value that the push gives at
its opening is counted too: a run that grew between its sizing and its opening ends the opening with
the same word. The word is given only for a layer
that she may see: steps 2 and 3 are made only when the layer's standing is `:yes`, so the size of a
layer that she may not see is neither read past 257 rows nor told.

What each step costs is in "The cost". What the three steps buy: a small run, which is the common read,
is one seek and its own rows in the layer's first call; no walk on a task is longer than 20,001 rows
whatever a tool asks; and no run of more than 20,000 places crosses to the runtime.

**The checks of ruling 12 are bounded too.** A read of what points at a thing needs one check for each
place of a farther order and each nearer layer. When `sources/reading` finds more than
`max-checks-of-one-read`, it wants none of them, and the read gives `:serve-a-read-that-large` with
`{:what :checks-of-one-read :most 40000}`. Under an open read, a change that makes the checks more
than the number ends the read: `:a-run-grew-too-large`, with the same `:what` and `:most`.

**The numbers of the build that bound a read** are in "Every number of the build". Each is the
builder's, kept and not decided, and the opener can give another at `open`.

## Each scenario, traced by hand

The layers of every trace: `:alice-notes` and `:team` are streaming and "members"; `:base` is
microbatch and "everyone". `:alice` holds a permission in the first two; `:bob` in `:team` only. "Own
rows of L" is S1, "her permission in L" is S2. A call is `foreign-select-async` with a `multi-path`
unless it says push.

**1. A place.** `{:asks [:place :note-41 :text] :asker :alice …}`.
- *Once, one layer* (`:layers [:team]`). Round 1, one call to `$$streaming-facts` with `{:pkey :team}`:
  own rows of `:team`, her permission, `(keypath ":team :note-41 :text")`. Three values. `reading`:
  made, "members", a permission row: `:yes`; nothing missing. `answer/whole`: the row's place-answer,
  or `{:holds :no-fact}`.
- *Once, three layers.* Round 1, three calls at once: to `$$streaming-facts` with `:alice-notes`; the
  same with `:team`; to `$$microbatch-facts` with `{:pkey :note-41}`, in which the base's own rows and
  her permission ride with the place, since every task holds them. Nine seeks. The nearest layer with
  a row wins.
- *Once, from a layer* (`:from-layer :alice-notes`, whose list is `[:team :base]`). Round 1: the call
  for `:alice-notes`. Its own rows give the list. Round 2: the calls for `:team` and `:base`.
- *Live, one layer.* Round 1: the push on the own rows of `:team`. Round 2: "members", so the push on
  her permission. Round 3: `:yes`, so the push on the place. Then `{:now … :leave-with h}`. An
  "everyone" layer takes two rounds.
- *Live, three layers.* Round 1: three pushes on own rows. Round 2: her two permissions, and the
  place in the base, whose standing is `:yes` already. Round 3: the place in the two others. Eight
  pushes (the table of "Live").

**2. An entity.** `[:entity :note-41]`: as 1, with form 3 on `(rows/entity-run layer :note-41)` in
place of the keypath. 12 places come back: the whole run. Live: the count is asked in place of the
rows, then the push on form 2. In the base the key is `:note-41`: one task.

**3. A run.** `[:run [:block-0001] [:block-0400]]`, 400 entities with one fact each, through three
layers, once. Round 1: one call for each streaming layer; four calls to `$$microbatch-facts`, one with
each task's key, the base's own rows and her permission riding in the first. `:team` holds 9 of the
places: its run is whole. `:alice-notes` holds none. The base's four parts hold about 100 each: whole.
`answer/whole` looks for each place's key in the layers in order, and in the base in the four parts.
Had one part held 257: that part's size, then that part whole.

**4. What points at a thing.** `[:points-at :parent :canvas-1]`, three layers, once. Round 1: three
calls, own rows and permission. The marks rows give `:parent` the stamps 700 in `:team` and 300 in the
base; `:alice-notes` does not mark it. Round 2: form 3 on `(rows/by-value-run :team :parent 700
:canvas-1)` with `{:pkey :team}`, and on the base's run with `{:pkey (rows/value-digest-of
:canvas-1)}`: one task. `:team` gives 2 places, the base 40. Round 3, `nearer-checks`: for the 2 of
`:team`, a check in `:alice-notes`; for the 40 of the base, a check in `:alice-notes` and one in
`:team`, less those that `:team`'s own order holds. Two calls, one for each nearer layer, each a
`multi-path` of keypaths. The answer: each place whose nearest row is in an order, or is a check's
row that holds `:canvas-1` (ruling 64); `:orders
[{:layer :alice-notes :not-marked true} {:layer :team :level-since 700} {:layer :base :level-since
300}]`. Live: the same rounds with one more for the permissions; each check is a push on one row,
whose value is nil while the nearer layer holds nothing there.

**5. A layer that she may not see, that is not made, that is named twice.** `:bob`, `:layers
[:alice-notes :team :nowhere :team :base]`, a place, once. `in-order`: `[:alice-notes :team :nowhere
:base]`. Round 1, four calls. `:alice-notes`: made, "members", no permission row: `:may-not-see`; the
place's row came in the same call and no function looks at it. `:nowhere`: no depot row: `:not-made`.
The answer: the place from `:team` or the base, `:layers` the four, `:left-out [{:layer :alice-notes
:why :may-not-see} {:layer :nowhere :why :not-made}]`.

**6. A setting written again under an open read, both ways.**
- *The base goes to "members"*; `:carol`, who holds no permission there, keeps a place live through
  `[:base]`. The push on the base's own rows calls back. Step 3: the row differs. Step 5: the meaning
  differs, "everyone" to "members". `reading`: her permission in the base is now wanted and is
  joined; the standing is `:not-known-yet`; the place's push, which she holds, stays held. Nothing is
  told. The permission's push is given with nil: `:may-not-see`. The place's push is left. The
  function gets `{:anew {:layers [:base] :left-out [{:layer :base :why :may-not-see}] :place {:holds
  :no-fact}}}`. Alice, who holds a permission in the base, gets an answer anew that says what it said.
- *`:alice-notes` goes to "everyone"*; `:bob` keeps a place live through `[:alice-notes :team]` and
  holds the own rows of `:alice-notes` and the push on his permission there, with nil. The own rows
  call back; the meaning differs; his standing is `:yes`; the place in `:alice-notes` is joined. When
  it is given: `{:anew …}` with the place from `:alice-notes` if it holds a row, and `:left-out []`.
  His permission's push is left: under "everyone" it shapes nothing.

**7. A permission given to her under an open read.** `:bob` as in 6, `:alice-notes` still "members".
Alice gives him a permission that says `:see`. The push on his permission's place calls back, nil to a
row. The meaning differs. `:yes`. The place is joined, and `{:anew …}` follows. His own act in that
layer would be refused by the gate; the read path does not read what a permission allows.

**8. The list written again under an open read.** `:alice` keeps a run live with `:from-layer
:alice-notes`; the list is `[:team :base]`. She writes it again as `[:base]`. The own rows of
`:alice-notes` call back; the meaning differs in the list. `in-order`: `[:alice-notes :base]`.
`:team`'s own rows, her permission there and the run there are left. `{:anew …}`. Written again as
`[:lib :team :base]`: the own rows of `:lib` are joined; when they are given, its standing says what
more is wanted; `{:anew …}` when nothing is missing.

**9. A key marked, and its mark taken off, under an open read of "what points at".** `:alice`,
`[:points-at :on :paper-9]` through `[:team]`; `:team` does not mark `:on`. The answer now: `:places
{}`, `:orders [{:layer :team :not-marked true}]`. A member marks `:on`; the gate gives it the stamp
900. The own rows call back; the meaning differs, no stamp to 900. The order `(rows/by-value-run :team
:on 900 :paper-9)` is joined: its count, then its push. `{:anew {… :orders [{:layer :team
:level-since 900}]}}`, with no place: the comments that were written before the mark are not in the
order, and `:level-since` says so (rulings 20, 43). A comment is written: the order's push sends a
`KeyDiff`; `{:changed {[:comment-3 :on] {:holds :value :value :paper-9 :layer :team …}}}`. The mark is
taken off: the meaning differs; the order is left; `{:anew … :not-marked true}`. Marked again, with
the stamp 950: another run; the comment of before is not in it.

**10. A place that a nearer layer wins with a fact that says "nothing here".** The base holds a value
at `[:fn-resolve :doc]`; `:alice-notes` holds a retract there. Through `[:alice-notes :base]`:
`{:holds :nothing-here :layer :alice-notes :fact … :stamp …}`. Live, a later change of the base's row:
step 5, `told-by?` is false, a nearer layer that she may see holds a row there; nothing is told.

**11. A thing that a farther layer points at and a nearer layer moves, or says again.** The base:
`:block-7 :parent` is `:canvas-1`. `:team` writes `:block-7 :parent` as `:canvas-2`. Both mark
`:parent`. `[:points-at :parent :canvas-1]` through `[:team :base]`, once: `:team`'s order for
`:canvas-1` does not hold `:block-7`; the base's does; the check `(keypath ":team :block-7 :parent")`
gives a row; its value is not `:canvas-1`; `:block-7` is not in `:places`. The same read for
`:canvas-2` gives `:block-7` from `:team`. *Live, opened before the move:* the check's push holds nil,
and `:block-7` is given from the base. The move: the check's push calls back, nil to a row;
`told-by?` is true, `:team` is the nearest; `of-places` finds `:team` holding a row whose value is
another thing: `{:changed {[:block-7 :parent] {:holds :no-fact}}}`. The base's order did not change.
`:team` moves it back to `:canvas-1`. In a streaming layer the row and the order change in one event
and call back as two pushes, in no order. The check first: its row now holds `:canvas-1`, so the
place is told from `:team`'s row; then the order, which is told again from the order's row, the same
fact; the check is left, since `:team`'s own order now holds the place. The order first: the same
place-answer, the check is left, and its callback reaches no read.

*Ruling 64, the case that it changes.* `:alice-notes` does not mark `:parent`. Alice wrote
`:block-7 :parent` as `:canvas-1` there, and the base says the same. Through `[:alice-notes :base]`:
the base's order gives the place; the check in `:alice-notes` finds a row; `holds-this-value?` is
true; the place is given, `{:holds :value :value :canvas-1 :layer :alice-notes :fact … :stamp …}`,
and `:orders` says `{:layer :alice-notes :not-marked true}`. *Live:* the base then moves `:block-7`
to `:canvas-2`. The base's order calls back with the removal. `reading`: no order holds the place
any more, so the check in `:alice-notes` is left. `told-by?` looks only at the sources that the read
still holds, finds no nearer row, and the place is told: `{:changed {[:block-7 :parent] {:holds
:no-fact}}}`. That is what a read asked once gives at that moment: the place is found only through
an order. The step must tell here whatever the check held: a change that the check's push had taken
and not yet called back never reaches the read once the check is left ("Live", "Why no put-together
value is kept").

**12. Two askers on the same thing; one leaves, the other stays, the last leaves.** Alice and Bob each
keep `[:place :note-41 :text]` live through `[:team]`. Alice: three pushes. Bob: he joins the own rows
and the place, which are open, and opens his own permission. `open-count`: 2 live reads, 4 pushes. A
write to the place: one callback, and each function is called once, under its own read's lock. Alice
asked `:with #{:writer}`, Bob did not: her change holds `:writer`, his does not. Alice leaves: her
permission's push is closed; 1 and 3. Bob leaves: the three are closed; 0 and 0; each close gives a
`DestroyedDiff`, which is nothing. A new asker opens new pushes and gets a fresh value.

**13. A caller that never leaves.** Its read and its pushes stay; Rama keeps them level; the gate of
each of its layers pays for them at each write. `open-count` shows them. Nothing bounds it but the end
of the process.

**14. A worker of Rama restarted under open reads.** For about a second, reads from outside throw (the
batch probe, "Seen on the way"; the check of the part before saw the same): a `read-once` in that
second gives `:reach-the-store`, and never throws. An open push is sent its value anew (batch probe
S7): the rows are the same, so nothing is told; a row that changed in between gives `{:anew …}`. If
Rama cannot serve a push ten times in 120 seconds it ends it (read, not run), and the read ends with
`:the-store-ended-a-push`. A stream topology that is handed its records again writes the same rows,
which is the part before's to hold; the read path gives nothing for a row that is the same. Run on
the probes' modules; the build's proof starts the store's module again under an open read (check 21).

**15. A very large run.** `[:run :first :last]` through `[:team]`, and `:team` holds 30,000 places.
Round 1: form 3 gives 257. The size: form 4 gives `[20001 k]`, and `k` is before `":team!"`. The read
gives `{:could-not :serve-a-read-that-large :what :places-of-one-run :most 20000}` after two calls,
four seeks and at most 20,258 steps on the layer's task. With 12,000 places: the size gives `[12001 k]` with
`k` after the run's end, or a count below 20,001; the run is read whole. For a layer that Bob may not
see: 257 rows are read and dropped, the size is not asked, and he gets the layer in `:left-out`.

## The cost

By the skill's figures (`SKILL.md`): a seek is 0.3 to 0.5 ms, a step to the next row 1 to 10 µs, each
call of a client a round trip. "Rounds" are round trips in turn, which is the latency; "calls" are all
round trips, which is the throughput. Every figure is derived unless it names a run. The asker is
signed in. "Rights" are a layer's own rows and her permission: 2 seeks and at most 6 steps.

**Asked once, in one layer.**

| The word | Rounds | Calls | Seeks | Steps |
|---|---|---|---|---|
| a place | 1 | 1 | 3: rights, the row | ≤ 6 |
| an entity or a run of n ≤ 256 places | 1 | 1 | 3 | n + 1, and ≤ 6 |
| a run of n > 256 places | 3 | 3 | 5 | 257 + 20,001 at most + n |
| a run across entities in the base, T tasks, each part ≤ 256 | 1 | T | T + 2 | n + T |
| what points at a thing, m ≤ 256 places | 2 | 2 | 3 | m + 1 |
| the same, m > 256 | 4 | 4 | 5 | 257 + 20,001 at most + m |

**Asked once, through k layers.** The calls and the seeks of each layer add; the rounds do not, since
the layers are asked at once. From a layer: one round more, the start layer's call first. What points
at a thing adds one round and, for each nearer layer, one call for each 2,000 checks and one seek for
each check: at most m × (k − 1) seeks, where m is the count of places in the farther orders.

**Kept live.** The opening costs the seeks above, as pushes: one round trip for each push, and one
select more before the push of each run (its count; two for a large run).

| The word, through k layers of which g are "members" | Pushes | Rounds to open |
|---|---|---|
| a place | k + g + k | 2, or 3 when g > 0 |
| an entity or a run | k + g + k, and T − 1 more for a run across entities in the base | one more: the count |
| what points at a thing | k + g + the layers that mark the key + one for each check | one more again: the checks |

After the opening a change costs Rama the work of the push, and the read path one walk over the
layers of the read for each place that the change names: k lookups in maps that are in memory, T more
for a part of the base. No call to Rama, unless the change makes a new source wanted.

**The worked case: a canvas of 5,000 through three layers, under ruling 12.** `[:points-at :parent
:canvas-1]` through `[:alice-notes :team :base]`; all three mark `:parent`. The base's order holds
5,000 blocks. `:team` holds a row at 40 of those blocks' places (moved or taken away) and has 20
blocks of its own in its order; `:alice-notes` has 5.

| Step | Calls | Seeks | Steps on a task |
|---|---|---|---|
| 1. Rights, three layers | 3 | 6 | ≤ 18 |
| 2. The first 256 of each order | 3 | 3 | 5 + 20 + 257 |
| 3. The size of the base's order | 1 | 1 | ≤ 20,001 |
| 4. The base's order whole | 1 | 1 | 5,000 |
| 5. The checks: 5,020 in `:alice-notes` (the base's and `:team`'s places), 5,000 in `:team` | 6 | 10,020 | 0 |
| In all | 14, in 5 rounds | 10,031 | about 25,300 |

By the skill's figures: 3.0 to 5.0 s of seeks in all. The two nearer layers are on two tasks, so the
longest path is 5,020 seeks, 1.5 to 2.5 s, and 5 round trips. The steps are 25 to 250 ms. What was
run, in-process with every row in memory: 2,000 rows of one task in one call in 14 to 21 ms (T8), so
the 10,020 checks would be about 0.1 s there; the sizing walk in 10 to 32 ms (U6); a run of 5,000
opened in 59 ms (the push probe). A real disk is between the two; C measures in-process only. Joined
without the checks (ruling 12's other answer) the same read is steps 1 to 4: 8 calls and 11 seeks.

Kept live, the same read holds 3 + 2 + 3 + 10,020 pushes. A block that changes in the base is one
callback and three lookups. A new block in the base is one callback, two pushes more, and one change
to the asker when both are given.

**What the runtime's memory holds for a live read.** For each run that it holds, every row of the
run: for the 5,000 rows of the order above, a key of about 120 characters and five fields, about 400
bytes each, 2 MB. A run of facts holds the plain values too: 20,000 rows of 200 bytes are 4 MB, and
at the limit of a value 1.3 GB (implied spec, "What the runtime's process holds"); the build's number
bounds the count of places and not the bytes (open point 4). For each push: Rama's own state for it,
not measured; the entry; the source as its key. For each read: its query and the set of its sources,
about 300 bytes for each source, so about 3 MB for the 10,028 above. Sharing keeps the rows from
growing with the count of askers; the sets of sources and the permissions' pushes grow with it. The
measure M3 prints the heap before and after.

## Partitioning efficiency

The read path chooses no placement: each table's is the module's. The table is filled for the reads,
through three layers of which the farthest is the base, asked once. "Seeks" are the tasks that one read
touches, summed, as the template counts. No inhabitant asks yet, so the proportions are this
session's guess, written down so that the table can be made again with real ones.

| A read through three layers | Proportion | Seeks at N = 1 | N = 16 | N = 128 |
|---|---|---|---|---|
| a place | 0.50 | 9 | 9 | 9 |
| an entity | 0.35 | 9 | 9 | 9 |
| what points at a thing, with c checks | 0.10 | 9 + c | 9 + c | 9 + c |
| a run across entities in a streaming layer | 0.04 | 9 | 9 | 9 |
| a run across entities that reaches the base | 0.01 | 9 | 8 + 16 | 8 + 128 |
| Weighted, without c | 1 | 9.0 | 9.15 | 10.27 |

Flat, but for the last line: a run across entities of the base has a part on each task, because the
module puts a fact of the base on the task of its entity. This part cannot change that. The module's
own answer to it is the facts by value, which the part before put on the task of the value's digest
so that "what is on this canvas" is one task at any N. If tools came to ask the last line often, the
weighted seeks would grow with the cluster; the receipt says so, and a tool that wants a canvas of the
base marks a key and asks what points at it.

## Design Decisions

- **Subindexing:** none; no table is declared here.
- **Colocation:** a layer's rights and the fact of a read are on one task in a streaming layer, and in
  a microbatch layer on the fact's task, since every task holds the rights' rows. So they are one call.
- **Which reads share a call, which a push:** asked once, by task; kept live, by source.

## State primitive selection

- No PState and no TaskGlobal: nothing of the read path runs in the module.
- `held`, with its two parts `:pushes` and `:reads`: one atom in the runtime's process. Not durable, and nothing rebuilds it:
  after a restart each caller asks again (ruling 25).
- The value of each push: Rama's `ProxyState`, in the runtime's process, kept level by Rama.

## Resource usage analysis

Disk: none more than the part before's tables. Memory of the runtime: "What the runtime's memory holds
for a live read", above. Memory and CPU of a task: for each open push, Rama's subscription and the
work of each write of that task's gate; read, not run, and the measures M2 and M4 give the first
numbers. Can it be less? The one large item is ruling 12's checks, one push for each; the settled
line 5 under "Open points" names three ways to hold fewer.

## The proof

Two homes, as the tree's map has them.

**The pure core, with no cluster: `test-store/softland/store/pure_proof.clj`**, which holds the door's
pure parts today and runs on the alias `:store`, Clojure alone. That alias stays the guard: the five
namespaces of `read_path/` load without Rama. Each check as "what it does — what a pass shows".

| # | What it does | What a pass shows |
|---|---|---|
| P1 | Each of the four words, with each kind of end, through `query/check`; then each fault once; then things that are no map, that throw when read, that hold 10,000 layers | The one form of a query. One word for each fault, read from the declaration. The check never throws |
| P2 | Compares the keys of `query/words`, `sources/of-word` and `answer/of-word` | A word of a query is declared in all three places or in none |
| P3 | `layers/in-order` on a list, a list that names a layer twice and its own layer, a start layer with no fact, with a fact, not known yet, not made, not seen. `layers/standing` on the seven lines of its table. `layers/meaning` on rows that differ and mean the same; on a list and on a mark that are written in a layer that she may not see; on a setting that goes to "members" for an asker who holds a permission. `answer/after-change` on a fact's step that joins her permission, and on one that leaves an order whose mark was taken off | Ruling 9's one function. Rulings 30 and 31, and what no fact reads as. That a row written again with the same word is no change. That a list or a mark of a layer that she may not see means the same before and after, so she is given nothing. That a setting which changes means another thing also when her standing stays `:yes`. That a step which joins or leaves a source that is no check owes the whole answer |
| P4 | The sources of each word in a streaming layer and in the base: table, partitioning key, path | The hand-off's table "Which key a client gives Rama", line by line. That two askers of one thing, with different `:with`, have equal sources |
| P5 | Plays the loop on a map that stands for the tables: `reading`, fetch what is missing from the map, again; for each trace of "Each scenario", once and live | The loop ends. Its rounds are those that "The pure core" derives, 5 at most. A read asked once wants the place with the rights; a live read does not |
| P6 | `sources/nearer-checks` on the case of trace 11, on a place that a nearer order holds, and on a place that two orders hold with a layer between them; then with the function put aside. `answer/of-places` on a check's row that holds another thing, that says "nothing here", and that holds the asked thing; `rows/holds-this-value?` on a UUID, a string with spaces, a decimal with trailing zeros, bytes that are no value | Ruling 12's one function wants a check in each layer nearer than the nearest order that holds the place, and in no other. Without it the answer is the runs joined. Ruling 64: the nearest row wins, and the place is given from it exactly when it holds the thing, by the test with which a gate puts a fact in that thing's order |
| P7 | `answer/whole` and `answer/of-places` on rows that the proof builds with `rows/fact-row`: no row, a retract, a value, bytes that are no value; nearer and farther; a store key's row; with and without `:with`; a row that two parts of the base hold | Ruling 14's four things. The nearest layer wins, also with "nothing here". Ruling 11. R4: a place-answer holds no writer and no permission unless asked. A row that every task holds is given once |
| P8 | `answer/told-by?` on a change in the winning layer, under a nearer row, in a layer that she may not see, of a store key's row | Which changes reach an asker |
| P9 | A player of the steps of "Live", with no cluster: tables as maps; for each push a value that it holds, a line of changes that it has not taken, and a line of callbacks that are not handled; a read with its sources, its mark and a screen (`:now`, then each `{:changed}` put in, each `{:anew}` in the place of all). The player moves one thing at a time: a write; a push takes its next value; a callback is handled, by `sources/reading`, the joining and leaving that it asks, and `answer/after-change`; a push that was asked for is served with the table as it is then; its arrival is handled, by `answer/after-arrival`. A source that the read left gives it nothing more. It plays EVERY order of these moves, for: the thirteen scenarios of the check's model (the crossing case of "Live" first: a nearer layer gets a row at a place while a farther order loses it; ruling 64's case; three and four layers; a layer that joins over a farther order); every list of up to three writes on one place in two layers, the nearer marking the key or not; and for a place and a run, a nearer layer that gets a row while the farther changes, with a layer that leaves and joins, and a setting that goes to "members" while a fact of that layer changes, for an asker who holds a permission and for one who holds none | In every order the screen at the end equals `answer/whole` of the tables at the end. It fails if `told-by?` counts a source that the step has left, if `{:anew}` is not carried by the mark, or if a place waits for a source behind the layer that wins. It plays the steps on the functions that `read_path.clj` calls; that Rama's client delivers in these ways and no other is the probes' (T5, C1 to C3), not this check's |
| P10 | `sources/over-the-limit?` on the four answers of U6; `rows/before?` on ASCII texts, on a text that is the start of another, on letters outside ASCII; `rows/same-row?` on two rows whose bytes are equal and not the same object, on nil and nil, on rows that differ in one field; `sources/named` with a removal of a key that neither value holds | The size rule. The order that a run's two ends are held to. Ruling 17 (c)'s test, and the hand-off's removal of a place that the run never held, as no change |
| P11 | Reads `words/words` and what each carries; compares with `act/reasons`, the declaration's reasons and the door's words | Each word is declared. No word of the read path is a gate's reason or a word of the door |

**The read path against the module: `test-store/softland/store/read_path_proof.clj`**, namespace
`softland.store.read-path-proof`, run as `clojure -M:store-module -m softland.store.read-path-proof`. It
starts Rama's in-process cluster as the other two proofs do (4 tasks, 2 threads, 2 workers), launches
`Store`, opens one door and one read path, and closes the cluster at the end. It prints `SHOWN` lines
and exits with a status that is not zero when a check fails.

**How the proof shows that nothing came.** A check that holds "the asker is given nothing" cannot
hold it by looking at once: a change that is still on its way would pass as none. So after the act
that must give nothing, the proof sends a second act that changes the same push, and waits until a
witness has that second change: a second live read of the proof's that holds the same push and is
told every change of it. The changes of one push come in order (T5), and one callback is handled for
every read that holds the push before the next callback begins. So when the witness has the second
change, the first one has been handled for the asker too, and she was given it or was not. Checks 14,
15, 17 and 18 hold "nothing" in this way.

The proof is the only asker: no view, no screen, no tool. Each act goes in through the door. The
proof's own hand, with a depot handle of its own, appends two kinds of thing that the door will not
send: bytes that are no value of the form under a right seal (check 4), and the batch of check 18,
which runs a second time by the way that the part `base-for-a-reader` made for the module's proof. It reads no
table round the read path for a thing that it shows; where it does, to compare, the `SHOWN` line says
so. It reads the read path's two maps from the fields of its type for what no call shows, as the
door's proof reads the door's stocks.

| # | What it does | What a pass shows |
|---|---|---|
| 1 | Opens a read path on the module, and on a name under which none runs | A read path, which prints as its name and nothing else. `:find-the-module`, by a real throw of Rama's client |
| 2 | Hands `read-once` and `read-live` each fault of P1, with the calls of Rama's client counted | `:read-the-query` with the check's word, and no call to Rama |
| 3 | For each gate: an act through the door writes a place, an entity of 5 keys, 12 entities, and 3 facts under a marked key that point at one thing. Each word, once | V2: the answer holds the value that an accepted act wrote; `:layer`; `:fact` is the fact id that the door's act-id and the fact's index make; `:stamp` is the stamp of the door's answer |
| 4 | A place with no fact; a retract; a string with spaces, a map, a UUID, a string of 60,000 bytes; bytes that are no value | Ruling 14's four things, each told apart. A value reads back as its maker gave it |
| 5 | One place asked with no `:with`, with `:writer`, with both | R4, observed: the first answer holds neither part, the third both, and they are the act's `:who` and `:permission`. For the system's making of a personal layer the two differ |
| 6 | The entity of a holder, and of the layer itself, asked without and with `:stores-own-facts`; a run across entities after a value was written; a `:place` under a store key without it | Ruling 11: no permission, setting, list, mark or request for locks in an answer that did not ask. With it, each is there. The last is turned away with no call |
| 7 | One place in three layers: only the farthest holds it; then a nearer one too; then the nearest holds a retract. A run that three layers hold in parts. `:from-layer` before and after the list is written again. A list that names a layer twice | V3: the nearest layer that holds a fact wins, also with "nothing here"; a place that only a farther layer holds comes from there; the answer says which layer won. VA5's last clause: a read from the layer uses the newest list |
| 8 | An asker with no permission in the middle layer; a layer that is not made; a start layer that she may not see | V5: `:left-out` names the layer and why; every other part of the answer equals the answer of the same query without that layer. Nothing of that layer is given |
| 9 | An asker who is not signed in; `:system`; a query with no asker, with both, with an asker of 300 characters | Ruling 31: she sees the base and nothing "members"; the others are turned away with their words |
| 10 | Trace 11, once: a farther layer points, a nearer moves, takes away, moves back. A nearer layer that does not mark the key and writes another thing, then the same thing. A nearer fact that says the same thing and was written before that layer's mark. Then the farther layer moves the place away. Facts written before the mark with no farther order. The mark off and on | Rulings 12 and 64 as built: hidden, hidden, shown from the nearer layer's order. Hidden; then shown from the nearer layer's row, with its `:layer`, `:fact` and `:stamp`, under `:not-marked` or a `:level-since` that is not earlier than its stamp. Not in the answer once no order holds the place. `:level-since` equals the stamp that the marks row holds, and no fact with an earlier stamp is given through that layer's own order |
| 11 | With no live read open, so that no value comes from an open push: counts the calls of `foreign-select-async` for a place through three layers; the same from a layer; an entity of 5 keys; a run across entities of the base; what points at a thing through three layers | 3 calls in one round; 3 in two; 1; one for each task; the rounds of "The cost". And for a `multi-path` of 300 places, that the values come in the paths' order |
| 12 | A read path opened with small numbers (`{:run-start-places 4 :max-places-of-one-run 10 :max-checks-of-one-read 6}`): a run of 3 places, of 8, of 12; a layer of 12 places that the asker may not see; `[:run :first :last]`; a read that needs 7 checks; a source of a kind that `path-of` does not know | The three steps, by their calls: 1, 3, and 2 with the word and no place. No word for a layer that she may not see. R3: the widest thing that a query can name ends in a word within a bounded count of calls; an unknown kind makes no call |
| 13 | A run across entities of the base, once and live, with and without `:stores-own-facts`; one write on one task | Ruling 29: the places of every task are in the answer; the base's depot row is there once; one write gives one change |
| 14 | Each word kept live, for each gate: the answer now; an act inside it; an act outside it; an act in a farther layer at a place that a nearer layer holds; 20 acts on one place in quick turn | V4: one change for the act inside, through one layer; through layers, where two pushes can cross, one or two that hold the same place-answer. None for the two others, shown as "How the proof shows that nothing came" says. R5: the asker ends at the value of the last act; no count of changes is asserted |
| 15 | Under open reads: the base's setting to "members" and back; a "members" layer to "everyone"; a permission given to an asker whose read left the layer out; the start layer's list written again; a key marked, a fact under it, the mark off, the mark on. For an asker who may not see a layer of her list: that layer's list written again, and the asked key marked in it. Through three layers, a permission given in the nearest while the farther orders hold places | Rulings 23 and 24: each `{:anew …}` holds the layers, `:left-out` and the places as the trace says. After a layer left, a write in it gives that asker nothing, and the other asker of the same push her change. A list or a mark of a layer that she may not see gives her nothing. A layer that joins gives one `{:anew}` when its checks are given, with the layer's own places in it |
| 16 | Two askers of one place with different `:with`; `open-count` and the two maps at each step of trace 12; a third asker after the last left, with the calls of `foreign-proxy-async` counted; 50 times, two threads open the same thing at once; 50 times, one joins while the last leaves, and a write follows | B5: one push for one thing; askers of different parts share it; it is closed when the last leaves, and its status is `:terminated`; one opening for two who come at once; nobody is handed a push that is closed: each who joined gets the write's change. After each turn the map of pushes holds no entry without a reader, and `foreign-proxy-async` was called as often as `close!` |
| 17 | A live asker of a place, then an act that the gate refuses at that place. For each value that the asker is given, the door's `answer-of` for the act of its `:fact`. The two maps after every asker left | R2, as far as a run reaches: a refused act gives nothing; every value given is of an accepted act; the read path then holds no reader and no push. That it holds no value is shown by reading its code: the two maps' entries have no field for one |
| 18 | By the way of rulings 49 and 57: a cluster of its own inside the proof's JVM, in which the three functions that the module's proof replaces are replaced, and put back after. A batch of the base runs a second time under an open read of a place that it writes | Ruling 17 (c): the function is called once for that place, with the first attempt's row. If the part `base-for-a-reader` left no such way in the tree, the check is left out and the map says so |
| 19 | With a function of the proof's in the place of a call of Rama's client: a select that throws; one that sleeps past a `:wait-ms` of 300; an opening that throws under an open read; an opening that never comes back under an open read. An `UngracefulTerminationDiff` handed to a push's callback, if the proof can make one of Rama's class; if it cannot, the `SHOWN` line says so and the map lists it under what the proof does not reach. A function that throws, and throws again when it is given `{:ended}` | `:reach-the-store`; `:get-an-answer-within-its-wait`; `{:ended …}` with each `:because`, after which the read holds no push and the map of pushes no entry of a failed opening. No call throws |
| 20 | Notes the thread of each call of a function, and whether two calls for one read overlap, under 200 acts from 4 threads | The function runs on a thread of Rama's client and never on the caller's. The calls for one read come one at a time |
| 21 | Starts the module again with Rama's test tool under an open read; then one act; a `read-once` straight after | An open read gets the act's change, or ends with a word; `read-once` gives an answer or a word. Neither throws |
| 22 | Reads every `.clj` under `src-store/` as text | R1, by a search: `foreign-pstate`, `foreign-select` and `foreign-proxy` are in `read_path.clj` and in no other file; `read_path.clj` holds no `foreign-append!` and no `foreign-depot` |
| 23 | Entities whose ids hold letters outside ASCII, and one outside the first plane: a run between two of them. An entity whose text is the start of another's (`:a`, `:a!`, `:ab`), and a layer whose text is the start of another's: the entity of the first, and `[:run :first :last]` of the first | The places that the read path gives are those that Rama's order holds between the two ends: `rows/before?` agrees with the table. A run of one entity, and of one layer, holds no place of a neighbour whose text begins with its text |
| 24 | Ruling 67, the function on Rama's thread. A function that calls `leave` on its own read inside its call; one that leaves another read; two reads on one push, each leaving the other inside its function, 50 times. A function that calls `read-once`, and one that calls `read-live`, inside its call, with the calls of Rama's client counted | `leave` gives `:left` and returns; the read gets no call after it; `open-count` ends at nothing; no thread stays. `:read-inside-an-askers-function`, with no call to Rama; the read whose function asked goes on |
| 25 | The opening. 50 times: a maker writes one place 20 times in quick turn while `read-live` opens on it; the function waits until the proof has put `:now` in place, then puts each change in. Then, once: a function that stays 2 s, and a `read-once` of another asker meanwhile | Each time the screen ends at the value of the last act: no change is lost between the opening of the pushes and `:now`, and none that is older than `:now` comes after it. The `read-once` gives an answer or a word and does not throw; the proof prints how long it took and holds it to no number |

**The contract's "When this build passes".** "asks for a place, an entity, a run of places, or what
points at a thing": 3. "through a list of layers": 7, 8, 10. "gets the value now and each change
after": 14, 15. "The read path has a proof of its own against the module": this file, and its row in
the proofs' map. "The four proofs of today stay and pass": the part before's, by ruling 1; this part
adds deftests to the pure proof and touches no check of the other three.

**R1 to R5, each as a thing observed.** R1: 22, and the proofs' map names the proofs' own handles as
the exception. R2: 17, and P9 for the reason that no value needs to be kept. R3: 12, 2. R4: 5, P7.
R5: 14, 25, and the docstring of `read-live`. Ruling 67: 20, 24, 25.

**What it changes outside the read path.** It appends to both depots through the door, and two kinds
of thing by its hand. It starts the module again once. It puts functions of its own in the place of
`foreign-select-async` and `foreign-proxy-async` for a while, to count, to throw and to sleep; each
`SHOWN` line that rests on that says so.

**What no check reaches.** A cluster of several machines, a network between the runtime and Rama, a
move of a task's leader, a killed process. Rama ending a push by itself, and Rama leaving a reader out
of a change: check 19 hands the read path the diff that Rama's pages name. The order in which two
pushes call back: no check can force it on Rama. P9 plays every order on the pure functions, and
checks 14, 16 and 25 only make a crossing likely. A number of tasks that
changes. Many askers and many pushes at once: 16 and 20 run a few threads. A restart of the runtime:
its memory is empty by construction. A tool, a view, an inhabitant. How fast anything is: the measures
print in-process numbers and prove nothing. The receipt's line is the contract's: "proven by a
hand-played asker, used by no inhabitant yet".

## The measures

One file, `test-store/softland/store/read_measures.clj`, namespace `softland.store.read-measures`, run
as `clojure -M:store-module -m softland.store.read-measures`: the alias of the module's proof, so the
root `deps.edn` does not change. It is a run that is no proof (ruling 36): it prints each number on a
line that starts with `MEASURED`, with what it is a number of, holds nothing to a number, and exits 0
unless it cannot run. Rama's in-process cluster, 4 tasks, 2 threads, 2 workers. Every act goes through
the door; every read through the read path. "The slowest 1 in 100" is the 99th of 100 when the times
are put in order. Each shape prints its count, the middle, the slowest 1 in 100 and the slowest.

| # | What is run | The load | Timed from, to | Printed |
|---|---|---|---|---|
| M1 | The streaming gate: one maker sends acts of one fact, a string of 100 bytes under a plain key, each to another place of one layer | 100 acts a second for 30 s, after 500 acts that are not counted; act i is due at start + i × 10 ms | (a) from the act's due moment, (b) from the call of `send-act`; to its return, which is the streaming depot's ack: the gate ran | the two sets of times; once with the key not marked and once with it marked |
| M1 | The base beside it: the same acts into the base | the same | from the due moment and from the call; to the moment at which `answer-of` with a bound gives "accepted" | the same; and how many acts a batch held, told from the stamps of the answers by the function of `gate/stamp.clj` that gives a stamp's batch, if the tree has one after the parts before. If it has none, the line says "not told: no function gives a stamp's batch" |
| M2 | M1's first shape, with live reads open on the layer | n = 1, 100 and 1,000 live reads of one asker who holds a permission in the layer, whose setting is "members": so n + 2 pushes, the n fact sources, the layer's own rows and her permission (`open-count` is printed). In two shapes: each read on another place that is not written; each on another run that holds the written places, the runs told apart by their last end | as M1 | M1's lines for each count and shape, and how many changes the askers got |
| M3 | The opening of a canvas of 5,000 facts: 5,000 blocks with ids that are neighbours, each with one fact under a marked key that points at one canvas. Asked as the run across the 5,000 entities, and as what points at the canvas; once and live; through one layer, and through three, where each of two nearer layers holds 50 facts at places of the canvas, 25 that say another thing and 25 that say the canvas | each of the 8 shapes 7 times, the first 2 not counted; in a streaming layer, and with the base as the farthest. Its read path is opened with `:wait-ms` 120,000: the live shape through three layers opens about 10,000 pushes in one call | from the call of `read-once` or `read-live` to its return | the 5 times of each shape, or the word that came in the place of an answer; the calls of Rama's client and the pushes that it opened (`open-count`); the heap in use before the opening and after it, after a collection; the time of `leave` |
| M4 | How late a push is: one live read of one place; one maker changes it, each act with another value | 10 acts a second, 500 acts, after 20 that are not counted; in a streaming layer and in the base | from the call of `send-act`, and from its return, to the first call of the asker's function with that act's value or with a later act's. Two acts of one batch of the base at one place give one change, with the later value (R5), so an act can have no call of its own | the two sets of times for each depot; how many acts were told by their own value and how many only by a later one. `send-act` returns at the streaming depot's ack and at the microbatch depot's append-ack; the door makes its append inside `send-act`, after it sealed the value |
| M5 | Nothing | — | — | one line: "not measured here: a real cluster, the base on one, a network, a second node" |

What cannot be measured here: the base on a real cluster, since none runs (the contract, C);
replication; a network between the runtime and Rama; what a push costs a gate under the load of many
machines. An in-process number is a floor for a round trip and says little of a seek, since every row
is in memory. The build session runs the file at the end and puts what it printed in `runs/` and in
the receipt (ruling 36).

## Every function, in the order to build

Pure first. "→" is what it gives. Each has a docstring that says what it takes, what it gives, what
must hold, and its limits (`src-store/CLAUDE.md`).

**`record/rows.clj`** — six functions that only a read needs, beside the part before's (open point 7;
ruling 66 counted five, and ruling 64 needs the sixth). Key texts are made in this file and nowhere
else.
1. `entity-run` [layer entity] → `[from to]`: the hand-off's `"<layer> <entity> "` to `"<layer>
   <entity>!"`. Must hold: both are ids within the limit.
2. `own-rows-run` [layer] → `[from to]`: the layer's own entity under the store's namespace, from
   `"<layer> <layer> :softland.store/"` to the same text with `0` in place of its last character. It
   reads `declaration/store-namespace`.
3. `before?` [a b] → true when the text a is before the text b in the order in which Rama keeps a
   table's String keys. By code point, which is the order of the bytes of UTF-8, and not by Java's
   `compareTo`, which puts a letter outside the first plane before U+E000. Run on a probe's table
   (C7: a space, `!`, `z`, `ä`, U+E000, U+FFFF, U+1F600, in that order); check 23 runs it on the
   store's.
4. `run-of-ends` [layer first-end last-end] → `[from to]`, or nil when the first end is after the last.
   `:first` is `"<layer> "`; `[e]` as a first end is `"<layer> <e> "`; `[e k]` is `place-key`; as a last
   end `[e]` is `"<layer> <e>!"`, `[e k]` is `place-key` and one space, so that the place is in the run
   (U4), and `:last` is `"<layer>!"`. Both texts begin with the layer's text and a space or `!`.
5. `same-row?` [row-a row-b] → true when both are nil, or each field of one equals the same field of
   the other, a byte array by its content. Never throws.
5b. `holds-this-value?` [row value] → true when the row holds bytes and `value-digest` of them is
   `value-digest-of` of the value: the test by which a gate puts a fact in that value's order. False
   for no row and for a retract. Must hold: the value passes `act/value?`. Ruling 64's comparison.

**`read_path/words.clj`** (`softland.store.read-path.words`; requires nothing)
6. `words`: the seven, each with `:from` and `:says`. `carried`: what a word can carry, by name. `word`
   [the-word carries] → the map with `:could-not`; nil for a word that is not declared.
7. `because`: the four reasons of `:keep-the-read-live`. `left-out-words`: `:may-not-see`, `:not-made`.

**`read_path/query.clj`** (`softland.store.read-path.query`; requires `act`, `door.declaration`,
`record.rows`)
8. `max-layers-of-one-read` 65. `default-numbers`; `numbers` [given] → the numbers, each a fixed-width
   integer above zero, a default for each that is left out or is none.
9. `words`, `shapes`, `rules`: the declaration of "The form of a query". `reasons`: read from them.
10. `rule-checks` (private): the code of each rule. Each takes a query that has the shape.
11. `check` [x] → `{:ok query}` or `{:refuse word :at part}`. Never throws.

**`read_path/layers.clj`** (`softland.store.read-path.layers`; requires `record.rows`,
`gate.first-act`, `door.declaration`)
12. `own-row` [own-rows layer k] → the row under one store key of the layer's own entity, or nil.
13. `made?`, `who-may-see`, `builds-on`, `marked-since`: [own-rows layer (k)] → what the row says, by
    `rows/read-value` and `rows/marked-keys-of`, with `first-act/starts-with` for no row. `builds-on`
    keeps only what passes `declaration/usable-id?`. None throws.
14. `standing` [layer own-rows permission-row signed-in?] → one of the four words of its table. The
    one function of rulings 30 and 31. Docstring: what a layer starts with is not decided.
15. `in-order` [query start-own-rows start-standing] → the layers, or nil. The one function of
    ruling 9. Docstring: not decided; the walk would be made here.
16. `left-out` [layer why] → `{:layer layer :why why}`. The one function of ruling 8. Docstring: not
    decided; what a read may tell of such a layer is made here.
17. `meaning` [query layer own-rows permission-row signed-in?] → what a layer's own rows and her
    permission row mean for this query, as a value that `=` compares: made or not, the setting,
    whether her permission row is there; and, only when her standing is `:yes`, the list of a start
    layer and the stamp of the asked key's mark.

**`read_path/sources.clj`** (`softland.store.read-path.sources`; requires `layers`, `record.rows`,
`gate.first-act`, `door.declaration`)
18. `own-rows-source`, `permission-source`, `place-source`, `run-sources`, `by-value-source`,
    `check-source` → a source: `{:gate :table :routing-id :path}`, and `:every-task true` where every
    task holds the rows. `:routing-id` is what Rama's client is given as its partitioning key; the
    word is the store's own, for the first argument of a read, so that "key" stays a fact's key. `:path` is `[:row key]`, `[:own-rows from to]` or `[:run from to]`.
19. `of-word`: a map from each word to a function [query layer own-rows task-routing-ids] → its fact
    sources in that layer.
20. `before-standing?` [mode source] → true for a read asked once and a source of one row or of a
    run's first read. The one predicate of open point 2.
21. `nearer-checks` [query layers standing known numbers] → the sources S6, or `:too-many`. The one
    function of ruling 12. Docstring: not decided; left out, the runs are joined.
22. `reading` [query known task-routing-ids mode numbers] → `{:layers :standing :wanted :missing :too-large}`.
23. `calls-for-once` [missing numbers] → the missing sources in groups of one table and one routing id, an
    `:every-task` source moved to a group of its layer, each group cut at `max-paths-in-one-call`.
24. `run-is-whole?` [rows-or-count numbers]; `over-the-limit?` [size to numbers] → true or false, from
    form 4's one or two values.
25. `named` [source names old new] → the keys that changed, or `:all`; a key whose row is the same
    before and after is not among them.

**`read_path/answer.clj`** (`softland.store.read-path.answer`; requires `layers`, `sources`,
`record.rows`, `gate.first-act`, `door.declaration`)
26. `counts?` [query key] → false for a store key that the query did not ask for. The one function of
    ruling 11. Docstring: not decided.
27. `place-answer` [query layer row] and `order-place-answer` [query layer thing row] → a place-answer.
    The one place where `:writer` and `:permission` are put in. `order-place-answer` takes a row of an
    order, or a check's row that holds the thing (ruling 64); its `:value` is the thing as the asker
    gave it.
28. `one-of-each` [parts key] → the row under a key that several parts of a microbatch layer hold: the
    one with the latest stamp.
29. `of-word`: a map from each word to how its rows are put together. `of-places` [query reading known
    places] → a map from place to place-answer, without the places that still wait. `whole` [query
    reading known] → the answer.
30. `told-by?` [query reading known source place] → true when a change of that source at that place is
    to be told to this asker. It counts a nearer layer's row only in a source that `reading` wants and
    `known` holds.
30b. `after-change` [query reading known source places meant-before meant-after joined left owed?]
    and `after-arrival` [query reading known source joined left owed?] → `{:tell nil, {:changed …}
    or {:anew …} :owed? true or false}`: what steps 5 and 6 of "Live" do for one read, once the
    joining and the leaving of that step are done and `known` is made again. Pure, so that the pure proof can play
    every order of the callbacks on them (check P9).

**`read_path.clj`** (`softland.store.read-path`; requires Rama, the five above, and `words`)
31. `ReadPath`, a type that prints as `printed` says. `what-threw` and `attempt`, as the door's.
    `within` [future ms] → `[:ok value]`, `[:threw info]` or `[:late]`; a push that is given late is
    closed.
32. `path-of` [source numbers step] → one of the five forms, or nil. The one function that makes a
    path.
33. `task-routing-ids` [read-path] → one routing id for each task of the microbatch tables, read now.
    Not `task-keys`: a key is the key of a fact.
34. `select-now` [read-path calls] → the values of each call's sources. `run-now` [read-path source
    first-rows] → a run's rows by the second and the third step, or a word. `read-once` calls it only
    for a run whose first read gave more than `:run-start-places` and whose layer has the standing
    `:yes`; for any other layer the first rows are dropped.
35. `open`. `read-once`.
36. `keys-of-diff` [diff] → `[[:put key] [:out key] …]`, or `:all`, by a processor of two interfaces;
    and what kind a diff is.
37. `join!` and `leave-sources!`: the two swaps of "How it is counted". `open-push` [read-path source]:
    the body of an entry's delay; for a run, its count or its size first, each a select that the next
    step is chained to. It gives a future of the push, or of a word, and waits for nothing: a callback
    of Rama's calls it under a read's lock.
38. `known-of` [read-path read] → a map from each source that the read holds, and that is given, to
    the value that its push holds now; nil when a push throws as it is read, which stops the step.
39. `tell!` [read change]: calls the function under the read's lock, if `:reads` still holds the read,
    with the thread marked for the length of the call; a throw of the function ends the read.
    `end-read!` [read-path read because carries].
40. `on-callback` [read-path source new diff old]; `on-arrival` [read-path source]: the six steps of
    "Live". They join, leave and tell as `answer/after-change` and `answer/after-arrival` say, and
    hold no rule of their own. `on-arrival` runs for each source that a step joined, when its push is
    given.
41. `read-live`, by "How a live read opens". `leave`. `open-count`.

**`test-store/`**: the deftests P1 to P11 in `pure_proof.clj`; `read_path_proof.clj`;
`read_measures.clj`.

## Words a session made, Sid's to change

None is from the rig's code, the server's or Inland's. "The read path", "asker" and "caller" are the
contract's and the implied spec's words; they are a session's too.

| Word | What it is |
|---|---|
| `softland.store.read-path`; `read_path/` with `query`, `words`, `layers`, `sources`, `answer` | The read path, and its five pure parts |
| `open`, `read-once`, `read-live`, `leave`, `open-count` | Its five calls |
| `:asks`; `:place`, `:entity`, `:run`, `:points-at`; `:first`, `:last` | What is asked; the four words of a query; the two ends of a layer |
| `:asker`, `:not-signed-in` | Who asks |
| `:layers`, `:from-layer` | Through which layers; the layer to take the list from |
| `:with`, `:writer`, `:permission`; `:stores-own-facts` | The parts of a row that are asked for; whether the store's own facts are |
| `:not-a-query`, `:part-not-known`, `:asks-no-word-known`, `:place-not-named`, `:entity-not-named`, `:run-not-named`, `:points-at-not-named`, `:asker-is-no-id`, `:no-one-asker`, `:layers-not-a-list-of-layers`, `:from-layer-is-no-layer`, `:layers-not-named-one-way`, `:too-many-layers`, `:part-of-a-row-not-known`, `:id-over-the-limit`, `:run-ends-out-of-order`, `:store-key-not-asked-for` | The check's seventeen words, each named for what is wrong; and the seven rule names of "The form of a query" |
| `:now`, `:leave-with` | What `read-live` gives: the answer now, and the handle |
| `:changed`, `:anew`, `:ended` | What the asker's function is given |
| `:left`, `:not-open` | What `leave` gives |
| `:live-reads`, `:pushes` | What `open-count` gives |
| `:left-out`, `:why`, `:may-not-see`, `:not-made` | What an answer says of a layer that gave nothing |
| `:place`, `:places`, `:orders`, `:level-since`, `:not-marked` | The parts of an answer |
| `:holds`, `:value`, `:layer`, `:fact`, `:stamp`, `:writer`, `:permission` | The parts of a place-answer. `:no-fact`, `:nothing-here`, `:value`, `:not-a-value` under `:holds` are the part before's |
| `:could-not`; `:find-the-module`, `:read-the-query`, `:reach-the-store`, `:get-an-answer-within-its-wait`, `:read-inside-an-askers-function`, `:serve-a-read-that-large`, `:keep-the-read-live` | The read path's seven words. The first is spelled as the door's and is the read path's own. The fifth was added by the check of this plan |
| "the read's turn" | The thing that lets one call of an asker's function run at a time for one read. Never "lock": a lock is an encryption key in this tree |
| `:because`; `:the-store-ended-a-push`, `:a-run-grew-too-large`, `:a-read-it-came-to-need-failed`, `:its-function-threw` | Why a live read ended |
| `:why`, `:at`, `:what`, `:most`, `:waited-ms`, `:module`, `:threw`, `:said`; `:places-of-one-run`, `:checks-of-one-read` | What a word carries |
| "a source"; `:gate`, `:table`, `:routing-id`, `:path`, `:every-task`; `:facts`, `:facts-by-value`; `:row`, `:own-rows`, `:run` | One value that Rama gives and can push, as data |
| "standing"; `:yes`, `:not-known-yet` (with `:may-not-see`, `:not-made`) | Whether an asker may see a layer, as the read path knows it now |
| "the layer's own rows"; "a check"; "the first read of a run"; "the size of a run"; "rank"; "round" | Words of the explanations |
| `:run-start-places`, `:max-places-of-one-run`, `:max-checks-of-one-read`, `:max-paths-in-one-call`, `:wait-ms`, `max-layers-of-one-read` | The numbers |
| the function names of the list above | Functions |

## Every number of the build, kept and not decided

Each is the builder's. The first five can be given at `open`.

| Number | Value | What it bounds | Why this one |
|---|---|---|---|
| `:run-start-places` | 256 | how many places the first read of a run takes | The window of 257 was run (U1 to U3). One act holds 256 facts at most, so an entity that one act wrote is whole in one call |
| `:max-places-of-one-run` | 20,000 | the places of one run in one layer, on one task, that a read holds | The largest run that the push probe opened as one push (Z2). The size's walk of 20,001 rows took 10 to 32 ms (U6) |
| `:max-checks-of-one-read` | 40,000 | ruling 12's checks of one read: places times nearer layers | The largest run through three layers. Not run as pushes at that count; M3 runs 10,020 |
| `:max-paths-in-one-call` | 2,000 | the paths of one `multi-path` | The count that was run (T8) |
| `:wait-ms` | 10,000 | how long a call of the read path waits for Rama before it gives a word | Ten times the second for which reads threw after a worker's restart; a tenth of the 100 s for which a path that Rama cannot serve hung |
| `max-layers-of-one-read` | 65 | the layers of one read | The start layer and the 64 that a list's fact can hold (`max-builds-on`) |
| the rounds at which `read-live` stops | 8 | a read that does not settle | Five are derived; three more for a list or a setting that is written while the read opens |

Not bounded by any number: the bytes of a run (20,000 values of 65,508 bytes are 1.3 GB), the count of
live reads, the count of askers, and how long a read stays open.

## Every explanation that the part adds or makes untrue

By file. Line numbers are those of head `c3e65c06`; the parts before will have moved some. A line is
replaced, never amended with a note of what it said before (`src-store/CLAUDE.md`). Sid's decisions
sections in `src-store/README.md` are his and are not edited (ruling 35).

| File | What changes, and what it will say |
|---|---|
| `src-store/README.md`, the first paragraph and the table of questions | The paragraph gains the read path: the client code by which a fact comes back out to a caller, once or kept live; its state is the memory of one process. The table gains one row: "How does a fact come back out to a tool, once or kept live, and what is it told of a layer that it may not see?" → the namespace docstring of `read_path.clj`, then `read_path/` for its choices and its words. The paragraph on the two aliases gains that the read path and its proof run on `:store-module`, and its five pure namespaces on `:store` |
| `softland/store/README.md` (the store's page): its first paragraph, "The parts", "How the parts relate" | The folder holds four things. Two rows in "The parts": `read_path.clj` (a client of the module; it opens the four tables of facts and reads nothing else; its state is who looks at what, and the pushes) and `read_path/` (the form of a query, the layers, the sources, the answer and the words; no state). One paragraph in "How the parts relate": **The read path reads** the gates' tables straight, with a partitioning key for each read, once by a select and live by Rama's push; it calls no query |
| The store's page, "Who reads a lock" (`:165-167`) | "In the runtime the door is the one client" becomes: the module has two clients in the runtime. The door reads only the locks of its own requests. The read path reads no lock: no row of the facts table names one |
| The store's page, "A direct read of the tables" (`:173-177`) | It says who reads the tables straight: the read path, for every fact that a caller is given; and the proofs. What such a read sees of a batch is what the part `base-for-a-reader` leaves it saying |
| The store's page, "Closing", its last sentences (`:395-400`) | "No read of this build meets it" is replaced by what the part `base-for-a-reader` writes there; this part adds only that the read path is the read that meets a replaced place, and reads it straight |
| The store's page, "Decisions this module stands on" | Rows for the entries of 11 and 12 October that show in the read path: 1 (`layers/standing`), 2 (`layers/in-order`), 3 (`read_path.clj`; the pushes), 6 (`query.clj`), 7 (`answer/place-answer`). Each row's "Where it shows" names the function; the wording of his decision is not written again as new text (ruling 35) |
| The store's page, "Words a session made" | One line that points to the read path's own page for its words |
| `softland/store/door.clj`, the namespace docstring, `:52` | "In the runtime the door is the module's only client" becomes: in the runtime the module has two clients, the door for the way in and the read path for the way out; neither calls the other |
| `door.clj`, `:111-113` | The line on W3 and W4 as open is replaced: a read's steps run in the read path (`softland.store.read-path`), which is what tells a caller that a fact changed. "The door reads no fact of the record for a maker, and its wait is one question about one act-id, with no subscription" stays |
| `softland/store/door/README.md`, `:223` | Item 8 becomes: the module has two clients in the runtime, the door and the read path |
| `softland/store/module/door_reads.clj`, the namespace docstring, `:73-78` | "In the runtime the door is the one client" becomes: the door is the one client of these three queries. "A select of one from outside is a second way to a row" becomes: the read path selects and watches the tables of facts from outside, by design; it reads no lock and no answer |
| `softland/store/module.clj`, the namespace docstring, "What a client of the module opens, by name" | Four lines more, after the part before named the two new tables: `$$streaming-facts`, `$$microbatch-facts`, `$$streaming-facts-by-value`, `$$microbatch-facts-by-value`, each "for the read path: select and push with a partitioning key". No depot, table or query is added |
| `softland/store/record/rows.clj`, the namespace docstring | "Inputs and results, by group" gains the six functions; "Callers" gains the read path's pure namespaces |
| `softland/store/record/README.md` | Unchanged in its map if it names no function; the builder reads it and changes it only where it says who calls `rows.clj` |
| `softland/store/read_path/README.md`, new | Parent links. The five files, each with its responsibility and "State: none". How the files relate. Boundaries: nothing here calls Rama; nothing here writes. Then the four sections of the contract's "Explanations": below |
| `test-store/README.md` | The list of runs gains the proof and the measures. The paragraph on what a pass shows gains: a pass of the fifth shows the read path in one process, with an asker that the proof plays; the measures are a run that is no proof |
| `test-store/softland/store/README.md` | The table of files gains two rows. A section "The read path's proof": how to run it, the 25 checks group by group, how it shows that nothing came, where it stands in for a call of Rama's client, what it changes outside the read path, what it does not reach. "The door as the module's one client, and two proofs" (`:529-540`) becomes "The module's two clients, and three proofs". The line "the three targets of speed and size" (`:344-345`) stays true of the module's proof and gains a pointer to the measures. The pure proof's section gains P1 to P11 |
| The head comments | `read_path.clj` gets one: "Before changing this file, read the plan of this part again", with this paper's path. The head comment of `module/door_reads.clj`, and of each other file with one that this part changes, gains this paper (rulings 39 and 58) |

**The read path's own page, `read_path/README.md`, holds:**

- **Decisions this build stands on.** The entries of 11 and 12 October by number, each with where it
  shows: 1 (who may see: `layers/standing`), 2 (which layers: `layers/in-order`; the nearest wins:
  `answer`), 3 (a read in the runtime, with Rama's push, which is no cache: `read_path.clj`, "Sharing"),
  6 (a query as data, a few words: `query.clj`), 7 (the writer within reach, given only when asked:
  `answer/place-answer`), 8 (the base read straight). The rest by pointer to `src-store/README.md`.
- **Engineering choices.** Each with its reason, each provisional: once or live by the call; a source
  as the unit that is shared; one call for each task; a layer's own rows as one run; her permission
  followed only under "members"; a read asked once reads the fact with the rights; the three steps of
  a run; the five path forms; no before is kept, the tests that stand in for it, and the one mark
  that a read holds; the function on Rama's thread under the read's turn, what a slow function stops,
  and the read that is turned away inside a function; the whole answer anew when what shapes a read
  changes; the numbers.
- **Not decided, kept for now.** The table of the next section.
- **Words a session made, Sid's to change.** The table above.

## Not decided: how each is built, and where the other answer would be made

None is decided here. Each docstring named below says "not decided".

| The thing | Built as | The one function where the other answer would be made |
|---|---|---|
| A read that left a layer out says so (the contract's list; ruling 8, provisional, for Sid) | `:left-out` names the layer and one of two words for why | `layers/left-out`: to say less, it gives less |
| How far a read walks from a layer (ruling 9, provisional, for Sid) | The one list: the start layer, then the layers of its own fact, no further | `layers/in-order`. The walk would also make S1 of each walked layer a source of rank 1, in `sources/reading` |
| The store's own facts in a tool's read (ruling 11, provisional, for Sid) | Left out unless the query holds `:stores-own-facts true`; any asker who may see the layer may ask | `answer/counts?` |
| "What points at a thing" through a list of layers (rulings 12 and 64, provisional, for Sid) | B3 holds: at a place that an order gives, the nearest layer that holds a row wins, and the place is given when the winner's value is the thing | `sources/nearer-checks`: left out, the runs are joined |
| One moment for all the layers of one read (ruling 13, kept as it falls out) | No lining up | None: it would be a query of the store, which the contract's picture rules out. The queue's ?6 stays open |
| What a layer starts with (the contract's list; ruling 6) | A layer with no setting fact reads as `first-act/starts-with` says | That function, the part before's; `layers/who-may-see` is its one caller here |
| One permission for each holder; "see" stays "see"; who may write a setting, a list, a mark | Nothing of them is code here: the read path reads whether a permission row is there, and reads no slot of it | — |
| Who asks is what the caller says (the contract, B2) | `:asker` is taken as given | `query/check`; sign-in would stand in front of the read path and change none of it |

## Open points for the build session

Only what no ruling settles. Each with its options and this session's recommendation.

1. **Once or live is the call, not a part of the query.** (a) As planned: `read-once` and `read-live`
   take the same query. (b) A part `:live true` in the data and one call. Recommended: (a). A function
   is no data, and with (b) two askers of one thing would hold two queries that differ.
2. **A read asked once reads a layer's fact in the same call as its rights.** So the runtime reads up
   to 257 rows of a layer for an asker who may then not see it, and drops them. (a) As planned: k calls
   for a place through k layers. (b) Rights first: 2k calls in two rounds, and nothing of a layer is
   read for an asker who may not see it. Recommended: (a); nothing is handed over, the runtime is
   trusted with the whole store (entry 4), and it halves the round trips of the commonest read. The
   other answer is `sources/before-standing?` giving false. It is a line for the receipt.
3. **`com.rpl.rama.Helpers/genHashingIndexKeys` in the runtime.** It is how the read path names one
   task (T2 to T4). Rama's pages show it for tests. (a) Use it, and ask Rama's team; Sid's line on Z6
   is "we will do it right as rama team will suggest". (b) A key partitioner of the store's own on the
   microbatch tables, which is a change of the module's declaration. (c) No run across entities in a
   microbatch layer: a word. Recommended: (a), with a line for the receipt. Only a run across entities
   of the base needs it.
4. **The numbers**, and that none bounds the bytes of a run. Recommended: as listed; a bound of bytes
   would need the size of a run's values before it is read, which no table gives.
5. **Settled by ruling 64, against this point's first recommendation.** The edge was: a nearer layer
   whose own fact at a place points at the same thing, written before that layer's mark or under a
   key that it does not mark, hid the farther place and was not shown itself. It is closed: the
   nearest layer that holds a counted row wins, and the place is given when the winner's value is the
   thing; the read gives it from the nearer layer's row, which the check has in hand ("What a read
   gives"). What stays, and goes to the row for the queue (P12) with the measures' numbers: a place
   is found only through an order, so a place at which a nearer layer says the thing and which no
   order holds is not in the answer; and the count is one push for each place and nearer layer,
   10,020 in the worked case, 40,000 at the limit. Ways to hold fewer, none planned, since rulings 12
   and 64 fix the count: one push on a nearer layer whole while it is small (the check of this plan
   costed it, its paper's points); the checks of one task in a few pushes on lists of places
   (`submap` is not such a path: T9); ruling 12's other answer.
6. **`:from-layer` when the asker may not see the start layer.** (a) As planned: its list is not
   used; the answer holds that one layer in `:left-out` and nothing else. (b) The list is used, and
   she is given the layers of it that she may see; she then learns what that layer builds on.
   Recommended: (a), marked provisional, for Sid, with ruling 8: it is what a read tells of a layer
   that she may not see.
7. **Six functions more in `record/rows.clj`** (five when ruling 66 was made; ruling 64 adds
   `holds-this-value?`), a file that the part before changes. Ruling 38 lets
   this part's build start on files of its own only. (a) This part's build adds the six as its last
   step, when the build of `what-the-gates-keep` has ended, and writes its pure code against them from
   the start. (b) The build session has them added in that part's build. (c) They live in
   `read_path/sources.clj`; then key texts are made in two files. Recommended: (a).
8. **The asker's function runs on a thread of Rama's client, under the read's lock.** (a) As planned;
   the docstring says what the function must not do. (b) An executor of the read path's own, with a
   queue of changes: a slow asker then delays nobody, and the queue is a thing that the read path
   keeps between two changes. Recommended: (a); a caller that needs a thread of its own takes one.
   Ruling 67 took (a). The check of this plan then ran what (a) costs (C4): a function that stays
   holds back the pushes of other askers on its thread, and selects and openings of other callers
   whose answers Rama's client gives on that thread. "On which thread the function is called" says it,
   and the check's paper puts it to the build session.
9. **A `:place` under a store key, asked without `:stores-own-facts`.** (a) As planned: turned away
   with `:store-key-not-asked-for`, with no call. (b) `{:holds :no-fact}`, which would say that a
   layer has no setting when it has one. Recommended: (a).
10. **The whole answer anew when what shapes a read changes.** (a) As planned: a setting, a permission,
    a list or a mark that changes gives `{:anew …}`. (b) Only the places that the layer held, as
    `{:changed …}`, with the layers beside them. Recommended: (a): such changes are rare, the whole
    answer costs what (b) costs at the worst, and one form fewer reaches a caller.
11. **A finding for the queue, decided by nobody.** One select with a path that Rama does not serve
    stopped a worker of the in-process cluster (T9). Entry 4 trusts each program that reaches Rama's
    ports with the whole store; this adds that such a program can also stop a worker with one call.
    The read path sends no such path. Whether the other sheet's network rule says more of it is
    Sid's.

## For the receipt

- **Provisional choices of this part, all the builder's:** the form of a query and its words; where
  the code lives; once or live by the call; a source as what is shared; the layer's own rows as one
  push; the three steps of a run and the five numbers; what a read asked once reads early (open
  point 2); the whole answer anew for a change of what shapes a read; the function on Rama's thread.
- **Not decided, as built:** the table above. Rows for the queue from this part: what a read tells of
  a layer when it was the start layer (open point 6, with P8); with P12, what ruling 64 leaves: a
  place is found only through an order; open point 11.
- **What Sid should know.** A member who may see a layer can ask for its permissions, setting, list
  and marks (ruling 11). A read tells an asker which layer it left out and whether that layer is made
  (ruling 8). A live read through three layers under ruling 12 holds one push for each place and
  nearer layer. Under ruling 64 a place can come from a layer that does not mark the key, when that
  layer's own fact says the thing and a farther layer's order holds the place; and it leaves the
  answer when the farther fact moves away, though the nearer fact still says the thing. A read can
  end at a moment that the caller did not choose, with a word, and the caller asks again. Whoever
  writes into the base waits its run to see her own fact, and so does a live asker. An asker's
  function that does not return soon holds back the reads of other askers (run: C4). The function
  can be called before its caller has `:now` in hand. A live read with a run across entities of the
  base would not notice a change of the number of tasks.
- **What was run, and what was not.** Run on Rama 1.6.0 in-process, on the probes' own modules: T and
  U above, and by the check of this plan C1 to C9 (`plan-check-probes-read-path/`): what a push holds
  inside its callback, the first callback of a push and the thread of a function chained to its
  future, what a callback that stays holds back, `close!` and a read inside a callback, the order of
  String keys outside ASCII, the cost of `foreign-object-info`. Played with no Rama: every order of
  the steps of "Live", by the check's model. Not run before the build: anything on the store's own
  tables; a push that Rama ends; a reader left out of a change; a number of tasks that changes;
  `Helpers` outside a test's cluster; how many threads the client of a real cluster has.
- **The line that the receipt opens with** is the contract's: "proven by a hand-played asker, used by
  no inhabitant yet".

## Design difficulty log

- **What is kept between two changes.** The hardest point, and the one that R2 turns on. The first
  design compared, at each callback, what the read gave before with what it gives after, with the
  other pushes as they are now. It keeps nothing, and it is wrong: two changes that cross can each
  look like no change (the crossing case; and a setting that opens a layer in the moment in which a
  permission is given). The plain repair keeps, for each live read, the value of each push as the
  read last handled it. That is a thing of our own kept beside the push, and I stopped there, as R2
  asks. What settled it: the read path does not need to know what the asker was told. It needs each
  test for "say nothing" to be safe by itself, and a lock that orders one asker's calls. The cost is
  that a place can be told a second time with the same answer when two changes cross. I am sure of
  the argument for a place and a run; for what points at a thing it rests on the same argument with
  "holds a row, by its order or by its check" in the place of "holds a row", and P9 plays the case
  that broke the first design. It is derived, not run, and the check should read it closely. (The
  check did: for what points at a thing the argument had a hole, where a step leaves a check whose
  change it has not handled; "Live" now has the argument as it holds, and what was played.)
- **A very large run.** Four ways were costed with the probes' numbers. No guard: Rama opens 200,000
  places as one push, and the runtime holds them. An exact count first: one call and n steps more for
  every run, and a walk with no bound for a huge one (113 to 168 ms for 200,000 places in memory). A
  window of 20,001 with no end, cut in the runtime: one call, and up to 20,001 rows cross for a run
  of 300. The three steps: a run of up to 256 places costs what it costs with no guard; a larger one
  two calls and at most 20,258 steps more; no walk is longer than 20,001 rows. The exact and bounded
  path for a large run does not exist: both orders of the two navigators throw past a small result
  (T8, U1). The call was close between the exact count and the three steps; the bound on the task's
  work settled it.
- **What a read asked once reads before it knows the asker's right.** One call for a layer against
  two. I took the one call, because what is read early is bounded and nothing of it is handed over;
  it is open point 2 because it is the kind of choice that a reader of the code would want to have
  been told.
- **The permission's push.** Ruling 23 names it among what a live read follows. Following it for each
  asker of the base would be one push for each asker for a fact that changes nothing while the base
  is "everyone". Following it only under "members" costs one round when a setting flips. Not close.
- **Ruling 12's checks.** One push for each place and nearer layer is what the ruling says, and it is
  the largest cost in the plan. I looked for a path that watches a list of places with one push;
  `submap` stopped a worker (T9). A nearer layer watched whole is cheap only while it is small. I
  built to the ruling and bounded the count.
- **The layer's own rows as one run.** Forced once seen: four seeks and four pushes become one of
  each, and the depot fact comes with the setting.
- **Nothing contested:** the place of the code, the words in place of a throw, the proof's shape. The
  door settled each.

