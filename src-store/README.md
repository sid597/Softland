# The store — source entry

This tree holds the code of the store that is built to stay. Today it holds one
namespace: [the door](softland/store/door.clj), `softland.store.door`. The door is
the part of the runtime that puts each act in its envelope and appends the act to
an acts depot. There is one door, with one entry for stream layers and for
microbatch layers. No caller names a depot, a gate or a class.

The gates, the record, the reads and the act's own code are still the rig's, in
`src/proposal/rig-2026-09-25/src/rig/store/`. The section "Below the door" says
which parts. "Act" is the word here; the rig's code says "offer" for the same
thing.

This file is a map of the code: what each function is and where it came from. The
reason for each change is one clause in that function's docstring.

## Run the proof

```sh
clojure -M:store -m softland.store.door-proof
```

One command, from the repository root, about 40 seconds. It starts Rama's
in-process cluster with the rig's module as it is (`rig.store.module/Store`, 4
tasks) and sends each act through the door, the seed also. The proof is
[test-store/softland/store/door_proof.clj](../test-store/softland/store/door_proof.clj).
It prints each thing that it showed on a line that starts with `SHOWN`, prints
those lines again together at the end, and exits with a status that is not zero
when a check fails.

Two of its scenes disturb the gates on purpose. The eighth holds the microbatch
gate for some seconds, with the door's wait set to 3 seconds. To set that scene
up it calls one private step of the door, `sealed-form`. The ninth fails two
events of the stream gate, so Rama logs two long stack traces there and starts
the worker again.

The alias `:store` in the root `deps.edn` is this tree's own classpath:
`src-store`, `test-store` and the rig's source, as `:replace-paths`. The root
`src` and `resources` are not on it, so a require of app code from the store
fails at load.

## The door's calls

| Call | Takes | Gives |
|---|---|---|
| `connect` | the cluster, and the module's name as a string | a door: seven handles and an empty memory |
| `make` | the parts from a caller: `:who :layer :permission :session :facts :stood-on :because-of :subjects`, and `:name` and `:claimed-when` if the caller has them | the act as said, with its name. Its values are still plain. |
| `send` | an act as `make` gave it | the answer `{:name :answer :reason :stamp}`, and `:batch` from the microbatch gate; or it throws "no answer" with the name |
| `make-and-send` | the parts | the answer |
| `answer` | a name | the answer recorded under it, or `:no-answer` |

An answer with no stamp is a refusal that was not recorded under the name. The
door gives one for an act that fails its examination, and for an act whose lease
the gate refused. A gate gives one for an act that it refuses on its face.

## Each function, and where it came from

Rig files are under `src/proposal/rig-2026-09-25/src/rig/store/`. "Step" is a
step of the door, as the namespace docstring numbers them.

| In the door | What it is | From the rig | What changed |
|---|---|---|---|
| `lease-size` | 64 locks for one lease in a stream layer | `client.clj:27` | nothing |
| `stream-tries` | 60 sendings to the stream gate, then the throw | `client.clj:315`, a default argument | it has a name |
| `microbatch-wait-ms` | 60 s for the microbatch gate's answer, then the throw | `micro_client.clj:264`, a default argument | it has a name |
| `fresh-memory` | what a door remembers | `client.clj:33` `fresh-door` | `:held`, the locks of an act with no answer, is now `:sealed`, its sealed form. `:made`, the people that the door made, is gone. |
| `connect` | the handles and the memory | `client.clj:42`, `micro_client.clj:27` | It takes the module's name as a string. It holds only the handles that the door uses. |
| `retrying` | a read done again while a worker starts again | `micro_client.clj:46` | nothing |
| `stream-settings` | a layer's settings in the stream record | `client.clj:470` `settings` | nothing |
| `settings-versions` | a layer's settings versions in the microbatch record | `micro_client.clj:109` | nothing |
| `settings-of` | a layer's settings in force | `micro_client.clj:102` | nothing |
| `class-of-layer` | step 2: the class of a layer, for one act | `micro_client.clj:455` `layer-class` | The door calls it; in the rig the caller does and hands the class in. An act that makes a layer takes its own class fact, as the gate reads it (`gate.clj:166`), before the `:by-layer` default. |
| `envelope`, `make` | steps 1 to 4 | `client.clj:71` `build`, `micro_client.clj:166` `build` | The caller gives no class. The door makes up no session (`client.clj:65` `default-session` is not taken). |
| `gate-for` | which gate decides an act, read off its name | `micro_client.clj:422` | The same rule. It is guarded, so an act that is not well formed cannot make it throw. |
| `refusal-of` | step 5: why the door does not send an act | new. It uses `envelope.clj:423` `parse`, `envelope.clj:465` `sealed-view`, `micro.clj:279` `parse-micro`, `gate.clj:64` `misplaced-setting?`. | The rig throws for a misplaced setting (`client.clj:102` `refuse-misplaced!`) and sends each other bad act for the gate to refuse. |
| `refused` | a refusal of the door: a no with no stamp | new | |
| `answer-from` | step 10: the answer in its one shape | `client.clj:302` `answer-of`, `micro_client.clj:489` | one shape for both classes |
| `value-indices` | the positions of the facts that the door seals | `client.clj:97` | nothing |
| `lease-act` | the lease act for an act | `client.clj:157` `lease-for!`, `micro_client.clj:277` `lease-offer` | One builder for both. It takes the class of the act that it serves. For a microbatch layer it still leaves out the permission of the operator (`micro_client.clj:283`, `gate.clj:43`). |
| `lease!` | send a lease act through `send` | new | If the lease gets no answer, the throw carries the act's name, with the lease's name beside it. |
| `held-for` | the lock id for each value fact, and each id's lock | `client.clj:167`, `micro_client.clj:309` `assign-by-grain` | one function for both; the map's `:keys` is now `:locks` |
| `refresh!` | take the session's unused locks into the pool, by query | `client.clj:123`, `client.clj:332` `lease-locks` | nothing |
| `take!` | take locks out of the pool | `client.clj:144` | nothing |
| `assign!` | the locks for a stream act, from the pool or a lease | `client.clj:176` | A refused lease gives `{:lease-refused answer}`. The rig seals the act under locks that it throws away and sends it (`client.clj:201-203`). |
| `take-locks` | the locks of a lease in a microbatch layer, by query | `micro_client.clj:295` | nothing |
| `lease-for-act!` | the locks for a microbatch act: one lease act for each act | `micro_client.clj:315` `write!`, `micro_client.clj:287` `lease!` | A refused lease gives `{:lease-refused answer}`. `write!`'s options (a lease by another actor or permission, a forced grain) are not here. |
| `seal` | the act with each value sealed under its lock | `client.clj:209` `seal-with`, `micro_client.clj:181` `seal` | one function for both; it throws when it holds no lock, as the microbatch one did |
| `sealed-form` | step 6: seal one time and keep the sealed form | `client.clj:225` `sealed` | The rig's stream door keeps the locks and seals again for each sending. The door keeps the sealed act, so each sending is the same bytes. |
| `ask-stream-gate` | steps 7 to 9 for a stream act | `client.clj:257` `offer!`, `client.clj:309` `offer-until-answered!` | After an error the rig reads the record by name and compares a digest (`client.clj:324-326`). The door does not: it sends again, and the gate answers from its record. |
| `own-answer?` | whether a result by name answers this sending itself | `micro_client.clj:225` | nothing |
| `await-answer` | ask the microbatch record by name until this sending has its answer | `micro_client.clj:238` | At its deadline the rig gives its last result, its own or not (`micro_client.clj:254-255`). The door gives nothing then. |
| `ask-microbatch-gate` | steps 7 to 9 for a microbatch act | `micro_client.clj:219` `send!`, `micro_client.clj:258` `offer!` | It takes only this sending's own answer, and throws "no answer" after the bound. |
| `answered!` | the door's memory after a gate's answer | `client.clj:240` | It runs for the answers of both gates. `:made` is gone. |
| `send` | steps 5 to 10: the one entry | `micro_client.clj:473` `offer-into!`, `micro_client.clj:315` `write!` | one entry for both classes; a refusal is an answer, not a throw |
| `make-and-send` | `make`, then `send` | new | |
| `stream-answer` | the stream gate's recorded answer under a name | `client.clj:269` `record`, `client.clj:302` `answer-of` | nothing |
| `microbatch-answer` | the microbatch gate's recorded answer under a name | `micro_client.clj:70` `lookup` | nothing |
| `answer` | the answer by name | new as one function | `:no-answer` is the one word for both classes |

## Not taken from the rig's doors

- The operator's acts: make a layer, a person, a group; grant, revoke, re-class,
  forget; open and close a session. They are converters, not the door.
- The seeds, and each read of a PState that only a test uses.
- `lease!`, `stock!` and `lookup-many`.
- `client.clj`'s `lookup` by digest and its `offer-until-answered!`. The stream
  side of the door calculates no digest.
- All of `promote_client.clj`. It comes with promotion.

The rig's three door files stay where they are. They still serve the rig's tests
and Inland. No file of the rig or of Inland changed for this tree.

## Below the door, still the rig's

- **The act.** All of `rig.store.envelope`, required in the door as `act`: the
  part sets and limits, the normal form and canonical text, the digest, names and
  tags, the id, the parse, the sealed view. One copy, which the gates and the door
  both use.
- **From `rig.store.locks`:** `seal`, `canonical-bytes`, `lease-ids`,
  `lease-name-of`, `max-lease`.
- **From `rig.store.gate`:** `misplaced-setting?`, and `exempt-actors` for the
  microbatch lease.
- **From `rig.store.micro`:** `parse-micro`, `parts-digest`, `envelope-fp`.
- **The module**, `rig.store.module/Store`: the two acts depots (`*offers`, which
  hashes the layer, and `*micro-offers`, which hashes `micro/route-key`); the
  stream gate and the microbatch gate; the PStates `$$layers` and `$$micro`, which
  the door reads directly for the class and for a stream answer; the queries
  `lease-locks`, `micro-lease` and `micro-lookup`.
- **Three words on the wire:** the scheme word `:offer` in a name, the depot names,
  and the class words `:by-layer` and `:by-entity`.

## What the proof shows, and what it does not reach

It shows, on a real run, scene by scene:

0. The sealed view of an act goes through the microbatch gate's parse as the
   sealed act does. This scene needs no cluster.
1. A seed made through the door: two stream layers and a microbatch layer. Each
   answer reads back by name, also for a name with no class.
2. One act by a person, with one value, into her own layer: the class read, the
   name made, one lease act for 64 locks, the value sealed in the depot's copy, a
   yes with a stamp, and a second door that reads the same answer by the name
   alone.
3. The same into a microbatch layer, through the same entry, with one lease act
   for one lock.
4. A no from a gate's decision, with its reason and a stamp, read back by name.
   One act sent two times: one decision, the same answer and stamp. A name that
   the caller gives: other content under a used name is the gate's `:name-taken`,
   and a name made for another layer is the door's `:mis-tagged`. A series of two,
   both named before the first goes, the second with a cause, subjects and a
   claimed time from the caller, and a retract.
5. Refusals of the door, each a no with no stamp, and no depot holds a record
   under one of their names: a fact with a part too many (both classes), a value
   and no session, a misplaced setting, a refused lease (both classes), and a
   layer that nobody made.
6. The locks: one lease for the 70 locks that one act needs; after a change of
   the lock grain, two values under one lock, in both classes; a name with no
   class that goes to the microbatch gate, and its answer by name; and a session
   that closes, after which the next act in it leases again.
7. A change of class between two acts. The next act that the door makes goes to
   the other gate. An act made before the change and sent after it gets the gate's
   `:class-mismatch`, an answer like each other.
8. The microbatch gate held. A record that the gate decided for another sealed
   form is not the answer to this sending: the door throws "no answer". A lease
   that gets no answer: the throw carries the act's name, with the lease's beside
   it. When the gate runs again, the kept sealed form goes again, the same bytes,
   and gets the gate's answer.
9. Sendings to the stream gate that fail. The door sends the same sealed bytes
   again, and the answer that it gives is the one in the record. With a bound of
   one sending the door throws "no answer" with the name, and the caller gives
   the act again.
10. A door that starts again takes its unused locks by query and makes no lease
    act.

It does not reach:

- the limits of the act: 256 subjects, a value 32 collections deep, a carried
  stamp below 2^62, a permission four deep; and a mark sent through the cluster;
- a change of class while an act is between its lease and its sending;
- a second lease when a pool runs out, and an act that needs more than the 256
  locks of one lease;
- a name with no class on an act that forgets a value of the microbatch gate;
- on the microbatch side: an append that fails, with its short wait and resend;
- two throws of the lease road: "could not lease enough locks", and no lock for
  an id when the locks of a lease do not come in time;
- a read that keeps failing until `retrying` gives up;
- two doors that write in one session at one time;
- promotion.

## Cases that the door does not handle

- A fact that the caller hands in already sealed goes as it is.
- `make` throws if `:facts` is not a collection.
- The sealed form of an act that never gets an answer stays in the door's memory.
- Two threads that send the same act at one time can each seal it.
- A door refusal for an act whose given name is not well formed carries that name
  as it was given.
