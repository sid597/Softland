# Door proof

[Parent: JVM checks](../README.md) · [Verification map](../../README.md).

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
