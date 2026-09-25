# Plan — stage 4, "promotion"

<!-- The rama skill's phase 1 (phase-1-plan.md, template artifact-plan.md),
written on 26 September 2026 from 02:38 IST by a fresh-context session
(Claude Opus 5.5, effort max) on branch rig-plan-promotion, worktree
/mnt/data/projects/Softland-rig-plan-promotion. Design only: no topology
code; one probe (runs/phase4-probe-sealed-box.clj and .txt).

Spec for this stage: SPEC.md (Sid's phase 4 verbatim, "What Rama showed",
binding); PROGRESS.md "Now" on main (the promotion sharpening, the names
sharpening, the two consequences, Sid's rule on order between the two
stores, rulings 7, 7b, 8, 9); RIG.md "Defaults taken overnight, not ruled"
(defaults 1, 2, 5 and 6 bind this stage); the formal model (model.clj
`send-read-out`, `forward`, `continue-promotion`, `refusal`,
`promotion-status`, `exempt?`, the `baseline` readings; scenarios.clj
`b-cases`). Built on: PLAN-stream-store.md and the phase 1 code (main rig
worktree, `envelope.clj` already reserves `:crossing` and `:landing` and
derives both names); PLAN-locks-and-forgetting.md as revised and validated
(e3989912); PLAN-micro-store.md at 848020f0 in Softland-rig-plan-micro,
with its PLAN_VALIDATION-micro-store.md (fixes F1 to F13, all taken as
applied); PLAN-read-exit.md at b0d5f138 in Softland-rig-plan-reads.

Vocabulary: "key" is a fact's key; "lock" is an encryption key. "The copy"
is the landed value in the target layer; "the source" the promoted value
in the owner's layer. -->

## Scope of this stage, in one paragraph

A person asks to move one of their values from a one-owner layer (their
own, or a hand or agent session of theirs) into a shared layer: a group,
or the base. Sid's phase 4: "A request act in the owner's layer; the
read-out opens the value through its lock on the owner's task and writes
the crossing fact there; a landing offer named from the request under a
scheme reserved to the store; pending with two states; a forget before the
read-out refuses, after it does not recall." This stage builds that
protocol on the two gates that exist: the stream gate (phase 1, with
phase 2's lease road) decides the request and the read-out on the owner
layer's home task, and the target's gate decides the landing: the micro
gate (phase 3) for a group and for the base after its re-class, the
stream gate for the base before it (default 6). It adds no topology, no
depot and no PState; it adds fields to `$$layers` and `$$micro-names`, one
query topology, one continuation in the stream gate's event flow, a
landing body in the one delivery function, and a kind of lease. Its tests
are the model's four B cases with their `:shown` sequences, both sides of
the read-out line, a retried request landing once, a crash between the
forward and the landing, a forget of the source after the landing, a
landing refused by the target, a promotion into the base before and after
its re-class, and the depot check after a forget before the read-out.

## The hard question, answered whole

**The problem.** Under default 1 nothing that could open a value sits in a
depot. The landing reaches the micro gate through `*micro-offers`, a depot,
so the copy must be sealed in it, and the lock that opens it must reach
the task where the target's gate decides the landing by a path the depot
never sees. Three forgets pull on that lock: a forget of the source before
the read-out must refuse the promotion; one after it must not recall the
copy (B cases 3 and 4: after Alice's value forget, and after Alice's
person forget, the group's copy stays open); and a later forget of the
copy, or a refused landing, must leave nothing that opens it (P6 widened:
the depot, lease rows, lock rows, anything the read-out leaves). The
stream gate's decision is one event with no partitioner inside (phase 1),
so the read-out cannot fetch anything from another task while it decides,
and the micro gate decides in a batch whose value work runs on the task
that holds the cited lease rows (phase 3, §A).

**The answer: the landing is sealed to a landing lease.**

1. **Who mints the opener.** The target's gate, before the request: the
   person's door makes a *landing lease* in the target layer, a lease act
   of count 1 bound to the one landing name the request will cause
   (`:landing <landing-name>`; names are made before the first gate, the
   names sharpening). The gate mints an X25519 key pair, not a symmetric
   lock, into one lease row beside its other leases: on the micro gate
   `$$micro-names [lease-name :leases 0]` on hash(lease-name); on the
   stream gate (the base while one-owner) `$$layers [base :leases s
   [lease-name 0]]` on the base's home. The row keeps the private key
   bare (`:under nil`), the public key, and the landing name it is for.
2. **What the door carries.** Only the public key, which opens nothing.
   The door takes it by the lease query (`micro-lease`, `lease-locks`),
   which for a landing row returns the public key and never the private
   one, and puts it, with the lease's lock id, into the request's control
   value. The request's depot record therefore holds ids, a layer, a
   permission id and a public key: nothing that opens anything.
3. **Who seals the landing, under which lock.** The stream gate, in the
   read-out's one event on the owner layer's home task, where the source's
   lock row is: it opens the source through its lock (phase 2's
   `open-value>`), draws a fresh 32-byte lock `K` for the copy (bound
   before the decision, with the event's nonces and an ephemeral X25519
   key pair, as phase 2 binds its nonces), seals the copy's plaintext
   under `K`, and seals `K` itself to the landing lease's public key (a
   sealed box: X25519 agreement with the ephemeral key, HMAC-SHA256 over
   the shared secret, both public keys and a fixed label as the wrapping
   lock, AES-GCM over `K`). Probed on this machine's JDK 21 with no new
   dependency (`runs/phase4-probe-sealed-box.txt`): the box opens with the
   lease's private key and with no other; 0.09 ms a key pair, 0.18 ms a
   box, 0.08 ms an unbox; 104 bytes a box. The ephemeral private key and
   the plaintext exist only inside the event.
4. **Where the opener sits until the landing's decision consumes it.** In
   the landing lease row, on the task where the target's gate does the
   landing's value work: the micro gate's arrival task, hash(lease-name),
   which is where the landing arrives (phase 3's route-key is the lease
   name of the first cited lock), or the base's home on the stream gate.
   `K` itself is nowhere at rest except inside boxes that only that
   private key opens: the landing in the depot, and the landing the
   read-out keeps for its replays (`:forwards`, below).
5. **What the landing's decision does with it.** The one delivery
   function gains a landing body: the lease row's private key opens the
   fact's box to `K`. From there the landing is decided as every offer
   into the target is: `K` opens the copy, the copy's own subjects come
   from the target's rules (the target's owner, none for a group, the
   base's no-owner row; the key's grammar over the copy; the landing's
   carried subjects, which the request's tool named), `K` is re-wrapped
   under that wrap into the record (group layers and the base keep locks
   in the record, ruling 7), the value digest is keyed by `K`, and the
   lease row is consumed in the same atomic group, whatever the decision
   (phase 2's `consume-locks>`, the one call site, V-F3).
6. **What happens when the landing is refused.** The lease row is deleted
   in the refusal's commit. The private key was the only opener of the box,
   so the landing's bytes in the depot and in `:forwards` open under no lock
   the store holds. A landing whose lease is already gone (its session
   closed before the landing was decided) is refused and *recorded*
   (`:landing-lock-gone`), so the promotion reads as refused, never as
   crossed for ever.

**The forgets against it, walked.**

- *A value forget of the source before the read-out.* Phase 2's forget
  deletes the source's lock row on the owner's home. The read-out, later
  on the same task, gets `{:erased-at d}` from `open-value>` and records a
  no, `:source-erased`. No copy was sealed, no box made, nothing forwarded.
  The landing lease was never used: its public key sealed nothing, so its
  private key opens nothing retained; it goes when its session closes.
  B case 1.
- *A person forget of the owner before the read-out.* Phase 2's fan-out
  destroys Alice's person lock on every task, the owner's home among them;
  in a one-owner layer the owner's lock is always required, so the source's
  wrap is closed and `open-value>` answers `{:erased-at d}`. The same
  refusal. The two events are ordered by the owner's home task's one
  thread, which is the order the model's B case 2 needs; "the lock store's
  order of a person's forget against a read-out" stays open in PROGRESS.md,
  and this is the rig's answer to it, reasoned and then tested.
- *Either forget after the read-out.* The copy is sealed under `K`, which
  nothing of the source's protects: not the source's lock (deleted by the
  value forget), not Alice's person lock (destroyed by the person forget),
  because the landing lease is bare. The landing is decided as it would
  have been. B cases 3 and 4: the copy is open. The read-out was a read
  that happened.
- *A forget of the copy after the landing.* `K` is then only in the copy's
  record lock (re-wrapped), so the copy's forget in the target reaches it:
  on the stream gate's base, phase 2's forget of a record lock (the
  excision the ruling names for record-default values); in a group, the
  micro gate's forget of a marked value or the operator's excision. After
  it the depot's landing, the micro depot's bytes and `:forwards` open under
  nothing: the lease row was consumed at the landing.
- *A person forget of someone the copy is about.* The copy's wrap is the
  target's (7b as written in shared layers), so it dies or survives with
  that person as any value in the target does; its depot bytes and
  `:forwards` follow the record lock, because the lease row is gone.

**The one-event rule and the batch.** The read-out needs nothing from
another task: the source's lock row is on the owner's home (phase 2: lock
rows sit with their values), `$$persons` is on every task, and the public
key came in the request. So the read-out is one event with no partitioner
inside. The forward comes after a commit boundary, `(|direct
(ops/current-task-id))` (stream.md: always a commit before an internal
append), so the crossing fact is durable before any landing exists. On the
micro side the landing is one more sealed act in a batch: it arrives on
its lease's task, where block 1 gathers the lease row and does the value
work, and nothing of it reaches the leader's fold but its skeleton (M3).

**Alternatives weighed and rejected** (the difficulty log has the working):

- *A symmetric landing lock minted by the micro gate and fetched by the
  stream gate in a hop before the read-out's event.* Works, with no new
  cryptography, but a bare lock then travels between tasks (phase 3's rule:
  a lock never leaves the task it is read on), and the stream gate's
  read-out depends on the micro store being reachable at that moment, a
  read of the other store in the promotion's path. Rejected for the sealed
  box, which needs nothing from the other store until the landing.
- *A lock minted at the read-out and kept in a row on the owner's task for
  the micro gate to read.* The micro gate can read that row but cannot
  delete it (a PState has one owning topology), so consumption needs a
  message back to the stream gate, two places consume one lock (against
  phase 2's V-F3), and between the landing and that message the copy's
  forget cannot reach the row. Rejected.
- *The copy's lock wrapped under the target subjects' person locks in the
  landing offer.* The store holds every person lock, so the depot would
  hold something that opens with a lock the store keeps; the copy's
  excision would not reach the depot's wrapped copy. Rejected by default 1.
- *The landing sealed under the source's lock, or a lease wrapped under the
  requester's person lock.* A forget after the read-out would recall the
  copy: B cases 3 and 4 fail. Rejected by the ruling.
- *A symmetric landing lock carried in the request, sealed under a stream
  lease, destroyed by the read-out.* The request's value would be a lock a
  read of the owner's layer could open, the door would hold the copy's
  future lock, and the read-out would have to forget part of the request.
  Rejected: more moving parts than the box, and a secret as a value.
- *An in-memory holder on the target's task.* Lost at a worker restart
  between the read-out and the landing, which would lose a crossed copy.
  Rejected, as phase 2 rejected it for the door.
- *A lock derived from a store secret and the landing name.* A long-lived
  opener of every landing in the depot, which no forget reaches. Rejected.
- *The read-out re-opening the source on a replay to re-send the landing*
  (the model's `forward` re-sends "while the source can still be opened").
  In the model the first send is atomic with the read-out's decision; in
  Rama the append follows the commit, so a crash between them followed by
  a forget would lose a crossed copy. Rejected for a stored forward, below.
