# Starter for the second round, 24 September 2026

The prompt for the session that runs the model's second round. Sid's words,
assembled by the session that reviewed the first round. Spent once the round
is reported.

---

I'm building Softland's store. On 24 September a session built a small
executable model of the store's rules in src/proposal/formal-model-2026-09-24/
and ran it against the rulings in src/proposal/frame-2026-09-15/PROGRESS.md.
Read PROGRESS.md, then the model's README.md and its three files under
src/formal/. Read nothing else for the design. Never read src/app/server/env.clj.

The model needs the Clojure CLI, Clojure 1.12.0 and test.check 1.1.0. If they
are not on this machine, install the CLI and let it fetch the two dependencies
before anything else. If the model cannot be run here, say so first, and
report only what you reasoned.

The model found six places where PROGRESS.md was silent or pulled against one
of my eight properties. Each is a config entry; the README's table lists them,
`ruled` for the literal reading and `amended` for the other. I have confirmed
all six amended readings as mine. They are the baseline now:
1. Layer belongs on the act; the gate refuses an act whose facts name another layer.
2. Re-classing a one-owner layer to by-entity moves it to the microbatch gate.
3. A promotion's landing offer is named before the first gate. Derive its name
   from the request's name under a scheme rather than carrying a second random
   name; keep the carried variant runnable so the two can be compared.
4. A per-value lock is wrapped under the value's own subjects, ruling 8's three
   sources applied to that fact; the act's subject slot is the union, for finding.
5. P4 is carved: a read as of T shows nothing admitted after T, except an
   erasure, which shows only its date.
6. P6's line for promotion is the read-out: the moment the stream gate reads the
   value out through its lock on the owner's partition. A forget before it
   refuses the promotion; a forget after it does not recall the copy. Treat the
   promotion shape, two steps with the crossing fact below, as a uniformity
   rule chosen once, not a temporary choice.

One rule sits under readings 6 and changes B and D below, and the README
should state it: order between the two stores exists only through stood-on. A
landing stands on its forward, so landing-after-forward is defined. A forget
and a forward on the same partition are ordered. A forget against a landing is
ordered by nothing. So any check one gate makes against the other store is a
read, stamped in the checking store, never atomic with the other store's
writes, and the model must never promise anything that needs such an order.
Say in the README where the model relies on this.

Then make these changes and run again. Where a change is a reading of the
rulings, make it a config entry so it can be flipped back.

A. Owner required. I have confirmed this one as mine too. In a one-owner layer
   (personal, hand, agent) the owner's lock is always required for every
   value's lock, whatever else the value mentions; 7b's survive default applies
   among the other subjects. In shared layers there is no person owner and 7b
   applies as written: a value about one person dies with them; a value about
   two survives one of them by default; marked dies with any. Adjust the P6 (b)
   standard and x1 to this. Check both directions: in Alice's own layer a note
   plus a mention of Bob both die with Alice; in the group a mention of Bob
   alone dies with Bob, a plain note about no one survives, and a value about
   Alice and Bob survives either one alone. Also run a third reading of A,
   which another session proposed and I have not accepted: in shared layers an
   unmarked subject gives no wrap at all, so only the owner's lock and marked
   subjects matter. Under it I expect a group mention of Bob alone to survive
   Bob's forget, and P6 should flag that. Report what it does.

B. The crossing fact. At the read-out the stream gate writes a fact on the
   owner's partition saying the value was read out for promotion. A read of a
   pending promotion shows two states: not yet read out, still forgettable;
   read out and not landed, already crossed. Extend P7 so "done" still never
   shows before the landing, and add a check that "crossed" shows if and only
   if the crossing fact is at or before the read's moment. P6's copy exemption
   should then follow from the crossing fact rather than from a separate config
   value; say whether it does.

C. Findable by name, strictly. Add a lookup that takes an offer's name and its
   layer and returns the answer without scanning every partition. For a
   by-layer layer that is the layer's home partition. For a by-entity layer the
   micro batch writes a name row on the partition the name picks, in the same
   commit as the admission. Check P1 through that lookup.

D. Permissions live in the layer they govern. A permission is a fact in the
   layer it grants writes into; a gate checks only permissions in layers it
   orders; an offer citing a permission from another layer is refused. Add a
   revoke op and check that no offer is admitted under a permission whose
   revocation is earlier in that store's order.

E. Name reuse. An offer that reuses a decided name with different content is
   refused by a digest of the offer. Add a reuse op and check P1 still holds.

Run the full matrix at 10000 histories on the fixed seed and at least two
more. Each new reading flipped back alone must bring back its own failure;
say if one does not. Report the coverage counts as before, and say which paths
were reached at 10000 but not at 1500; the nine-op race on the landing check
was one.

Report in plain words: what holds, what fails, the smallest failing history
for each failure, and which line of PROGRESS.md each change sharpens. Mark
what you ran versus what you reasoned. Update the model's README with the new
table and traces. Don't build the rig, don't change PROGRESS.md, don't commit,
and ask me before deleting anything.
