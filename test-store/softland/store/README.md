# Store proofs

[Parent: JVM checks](../README.md) · [Verification map](../../README.md).

| File | What it checks | State that it owns or changes |
|---|---|---|
| [act_proof.clj](act_proof.clj) | [The act](../../../src-store/softland/store/act.clj): its declared shape and rules, the check that reads them, its names, the stand-in sealed form and the digest. It needs no cluster. | One atom: the lines that it showed. |
| [door_proof.clj](door_proof.clj) | [The door](../../../src-store/softland/store/door.clj), against the rig's gates on one fresh in-process cluster. | One cluster for one run. It also changes state outside the door; the list below says how. |

## The act's proof

Each check is a case: what it is about, a function of an implementation, and what
that function must give. The proof gives each case the act's own functions.

What it checks, group by group:

1. The two declarations, the shape and the rules, each written out a second time;
   and that the check reads them: for each part that the shape of an act names, an
   act without the part, and an act with a thing of no shape in it, get the reason
   that the declaration gives.
2. Acts that pass, each in its one form: one sealed value by a person into her own
   layer; the same act as said, through its sealed view; an act of store-key facts
   only; a retract; several facts with a mark on one; an act that the store places
   itself, under a name with no class; an act into a layer that is placed by
   entity; an act with each optional part, whose one form has plain vectors and
   longs and parses again to itself; and the least act.
3. One refused act for each fault, with its reason. Each differs from an act that
   passes in one thing only. Each reason word comes from an act that earns it.
4. The order of the checks, which is the order of the declarations: an act with
   two faults gets the reason of the shape before the reason of a rule, and of the
   earlier part or rule before the later one.
5. Nothing throws: the check, the sealed view and the test of a name are each given
   things that are not acts, and each gives back data.
6. The limits at their edges: the subjects, the collections around a store key's
   value, a carried stamp, and the depth of a permission.
7. Names: a made name and its tag, the name for an act that the store places
   itself, the two names that the store makes from a request, and what is no name.
8. The stand-in sealed form: an act as said is refused as it is and taken through
   its sealed view.
9. The digest: what it leaves out, and that a change of each part that it covers
   gives another digest. What the digest does not see is a check of its own: two
   acts that differ only in a sealed value have one digest.
10. Ids and values: what is a value, the one normal form, and the canonical text.

### The same cases through the rig's envelope

```sh
clojure -M:store -m softland.store.act-proof beside-the-rig
```

This run gives the same cases to `rig.store.envelope`, and prints one line that
starts with `DIFFERS` for each case where the two give different things. It
asserts nothing, and it exits with status 0: a difference is a finding, not a
failure. Nobody made the two agree.

To give a case to the rig's envelope, the run puts two words back on the way in
and on the way out: the part `:based-on` is the rig's `:stood-on`, and the scheme
word of a made name is the rig's own. The rig's parse takes a gate, so each case
goes to the gate that the class of its name is for. `nil` on the rig's side of a
line means that the rig's envelope has no such definition.

Its last two lines are about the digest of one act. The two files give different
text. The second line shows the cause: the act's own function, given the act in
the rig's words and the rig's secret, gives the rig's digest.

### What the act's proof does not reach

- an act after Rama wrote it to a depot and read it back: each case is a value
  of this process;
- facts that are a sequence with no end, for which the check does not come back;
- an act of many facts, and how long the check takes;
- that two different values never give one canonical text, beyond the examples;
- the id of a made name, beyond its version and that two ids differ;
- the use of the act by the door or by a gate: nothing uses it.

## The door's proof

[door_proof.clj](door_proof.clj) owns one fresh four-task in-process cluster with
the rig's module, `rig.store.module/Store`, as it is, for the length of one run.
It sends each act through [the door](../../../src-store/softland/store/door.clj),
the seed also. It builds the operator's acts itself, because they are converters
and not the door, and it reads the two acts depots with Rama's own calls, because
the door has no read of a depot.

What it checks, scene by scene:

0. With no cluster: the microbatch gate's parse takes the sealed view of an act as
   it takes the sealed act, with the same digest. The door's examination rests on
   this.
1. The seed, made through the door: two stream layers and a microbatch layer. Each
   answer reads back by name, also for a name with no class.
2. One act by a person, with one value, into her own layer: the class read, the
   name made, one lease act, the value sealed in the depot's copy with no plain
   value, a yes with a stamp, and a second door that reads the same answer from the
   name alone.
3. The same into a microbatch layer, through the same call.
4. A no from a gate's decision, read back by name. One act sent two times, with
   one decision. A name that the caller gives: other content under a used name,
   and a name made for another layer. A series named whole before its first act
   goes, with the parts that only a caller knows and a retract.
5. Refusals of the door, each a no with no stamp and no record in a depot: a fact
   with a part too many, a value and no session, a misplaced setting, a refused
   lease, a layer that nobody made; in both classes where the class matters.
6. The locks: one lease for more locks than the usual lease, one lock for the act
   in both classes after a change of the grain, a name with no class that goes to
   the microbatch gate, and a session that closes.
7. A change of class between two acts, and an act made before the change and sent
   after it.
8. The microbatch gate held: a record that the gate decided for another sealed
   form is not the answer to this sending, and a lease that gets no answer.
9. Sendings to the stream gate that fail: the same sealed bytes go again, and the
   answer is the one in the record.
10. A door that starts again takes its unused locks by query.

Some scenes change state outside the door, and that fixes their order:

- Scene 8 pauses the microbatch topology and rebinds the door's wait to 3 seconds
  for two calls. To set its stage it calls one private step of the door,
  `sealed-form`, so that the door holds a sealed form that the gate did not decide.
- Scene 9 arms the rig's fault hooks (`rig.store.inject`), which are atoms that the
  whole JVM shares, and rebinds the door's bound to one sending for one call. Each
  armed fault fails one event of the stream gate, so Rama logs a long stack trace
  and starts the worker again. So it comes near the end.
- Scene 10 is last: its door takes the unused locks that the first door still
  holds, and the first door is not used after it.

The suite does not reach:

- the limits of the act (the number of subjects, the depth of a value, the size of
  a carried stamp, the depth of a permission), and a mark sent through the cluster;
- a change of class while an act is between its lease and its sending;
- a second lease when a pool runs out, and an act that needs more locks than one
  lease gives;
- a name with no class on an act that forgets a value of the microbatch gate;
- on the microbatch side, an append that fails, with its short wait and resend;
- the two throws of the lease road: too few locks after many rounds, and no lock
  for an id when the locks of a lease do not come in time;
- a read that keeps failing until its retry gives up;
- two doors that write in one session at one time;
- promotion.
