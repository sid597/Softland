# Plan Validation — stage 2, "locks-and-forgetting" (the 26 September revision)

<!-- Rama skill phase 2 (references/phase-2-plan-validate.md, template
artifact-plan-validation.md). Validator: a fresh-context session, Claude Opus
5.5, effort max, 26 September 2026, 02:16 to 02:58 IST, on branch
rig-plan-locks. Default verdict FAIL. Plan validated:
PLAN-locks-and-forgetting.md at cbd2bb16 (revision parts 1 to 8).

Read in full or in the sections cited: the plan's "Revision, 26 September",
"The shapes", "The door and the lease road" (all of it), the gate event
(steps 1 to 11), "No input can make topology code throw", `lease-locks`,
"Partitioning efficiency", L5, L23 to L26, "What later stages consume"
(stage 1 suite note); SPEC.md phases 1 to 8 and "What Rama showed" 1 to 5;
the main rig worktree's RIG.md "Defaults taken overnight, not ruled" 1 to 8;
CONCLUSION.md "Eight rulings" R1 to R8 and "Also going into the model".
Gathered by two read-only Sonnet gatherers, facts only, spot-checked where
a finding turns on them: stage 1's refusal order and the tests that assert
it (gate.clj, envelope.clj, client.clj, stream_gate_test.clj,
envelope_test.clj; gate_test.clj was not opened); model.clj's gate order,
`digest-of`, `wrap`, `lock-for`, forgets and `read-as-of`, and
scenarios.clj's eight A cases.

Not re-traced tonight, and counted as such in the verdict: the parts of the
plan the revision did not change (the forget acts' effects L10/L16/L17, the
person fan-out's mechanics, `read-as-of`'s reads, the ledger, `:by-stamp`)
were checked only where the revision's road runs through them; the
template's minimality and throughput sections are answered for the
revision's new mechanisms only. PROGRESS.md "Now" was not re-read tonight;
rulings 7, 7b and 8 were checked through the model (`wrap` under
`:owner-required`, `:lock-subjects :fact`) and CONCLUSION, not through their
text. -->

## Verdict

**minor-fail.** Four failures, each fixable by specific edits to named
sections; fixed in the plan as F1 to F4, each marked `[V-F n]` where it
lands. None needs a redesign: the lease road, the delivery seam, the
re-wrap, the split digest and the consumption rule stand; F1 changes what
the parts digest covers and what the record path does, F2 places the base
under rules the plan already has, F3 splits one function into two, F4
completes a list.

| Fix | Ask | What failed | Where fixed |
|---|---|---|---|
| F1 | 1, 2, 5 | lock ids in the parts digest refuse an honest resend under a newly leased lock as `:name-taken`; the record path then left such a resend's lease rows alive, and after a forget they open the forgotten value's plaintext in the depot | "The shapes" (parts digest), door step 4, "The digest and the resend check", "Consumption of lease rows", "The missing lock", gate event step 3, cost row (c), L26 |
| F2 | 3, 4 | the base (default 6, R8) cannot be made, leased into or wrapped: the plan puts it in stage 3, refuses a making act whose owner has no person lock, and seals lease rows under the layer's owner | "The shapes" (wrap table, lease row), "Re-wrap", "Lease rows", `lease-locks`, the lease act, L23, tests |
| F3 | 7 | the holder body removes its entry inside `deliver-lock>`, so a face `:no-such-lock` on a later lock of the same act, or a discarded event, consumes a lock the plan says is untouched | "The delivery function", "Consumption of lease rows", interfaces |
| F4 | 6 | the list of phase 1 expectations that change is incomplete (it names two groups; at least six places change) | "What later stages consume", stage 1 suite note |

## The revision's road, traced first

### Trace 1. A resend after the door lost its lease pool — FAIL, fixed by F1

**Source.** SPEC "What Rama showed" 4: "The client can get an error for an
offer that went in. An offerer resends under the same name and takes the
answer from the record." CONCLUSION R1, riders: "It is never taken over the
sealed bytes, because a resend gets sealed again and its bytes differ." "To
check a resend, the gate opens it with its own lock, recomputes the digest
under the lock it recorded the first time, and compares." Default 1
(builder A): "a lease row is consumed at decision whatever the decision
(admitted, it becomes the value's lock; refused or answered from the
record, it is destroyed)".

**History.** Layer `:alice` (personal, per-value grain), session `:s1`.

1. The door offers lease `N1 = [:alice :by-layer :offer u1]`, count 4. Yes;
   lock ids `[N1 0] .. [N1 3]`; rows at `[:alice :leases :s1 [N1 i]]`.
2. The door calls `lease-locks [:alice :s1]`, holds K0..K3 in memory.
3. The door seals note "hello" under K0 and appends offer X, name `NX`,
   one sealed fact citing `[N1 0]`.
4. The gate decides X: yes. `[N1 0]` consumed; K0 re-wrapped under
   `{:required [:alice]}` as lock row `[:alice :locks [N1 0]]`; the row
   keeps the sealed bytes and `HMAC(K0, "hello")`.
5. The ack is lost; the door process restarts. Its memory held K0..K3; the
   offerer kept X's plaintext (SPEC item 4 obliges it to resend).
6. The restarted door calls `lease-locks [:alice :s1]`: `[N1 1..3]` come
   back; `[N1 0]` does not (consumed in step 4). It cannot seal under K0.
7. It seals "hello" under K1 (or a fresh lease's lock) and resends X under
   `NX`, citing `[N1 1]`.

**What the plan as validated does.** Plan lines 243-245: "`:lock-id` stays
in, so a resend must cite the same locks (L26)"; L26: "lock ids in the
parts digest make a resend's cited lock the recorded one." The resend's
parts digest covers `[N1 1]`, the record's covers `[N1 0]`: they differ.
"The record path" (lines 439-440): "the parts digest differs →
`:name-taken`, on the face". The honest resend is refused as a reused name
with other content, and the offerer never learns that X was admitted.
SPEC item 4 is broken for any door that loses its memory between the
append and the answer, which is exactly the case item 4 exists for.

A second failure on the same road: the `:name-taken` is a face refusal, so
"the cited rows are left alone" (line 475-476). `[N1 1]` stays live and
opens the resend's depot bytes. If X's note is forgotten after step 4 and
the lost-pool resend comes after the forget, the depot holds the forgotten
plaintext under a live lease row until `:s1` closes. P6 widened ("nothing
retained opens or confirms a forgotten value", CONCLUSION "Also going into
the model") fails in place 1 and place 2 for that interval, and a crashed
door may never close `:s1`.

**Why the lock ids cannot stay in.** R1's rider names two locks, "its own
lock" and "the lock it recorded the first time", which differ only when a
resend may be sealed under a lock other than the recorded one. The model's
`digest-of` (model.clj 532-535) is `(hash (dissoc offer :name :digest))`
over the offer, whose facts carry plaintext stand-ins and no lock (locks
are attached at `admit`, model.clj 488): the model's content test has no
lock id in it. The plan's reason for keeping them (a write-free record
path) is answered by builder A's own sub-pick, which already destroys the
lease rows of an offer "answered from the record".

**F1, the fix applied.** The parts digest leaves out every `:sealed` and
every `:lock-id`. The record path for a recorded yes, per value fact of the
resend (the parts digest matched, so the facts correspond by position):

- the recorded lock `R`: the lock row, or the row's `:lock`, unwrapped with
  `$$persons` (nil when forgotten or its wrap is closed);
- the resend's own lock `O`: `R` when the cited id is the recorded id (a
  door that kept its locks), else `deliver-lock>` of the cited id under the
  offer's own session (nil when missing);
- both open → open the resend's bytes with `O`, recompute `HMAC(R, plain)`,
  compare with the row's `:digest`; no open or a difference → `:name-taken`
  on the face, nothing written;
- either nil → that value is not checked (after a forget, the rider's price;
  a missing own lock, see below);
- every value checked or skipped → the recorded answer, and a `NONE>` on
  every cited lease row under the offer's session, delivered or not: the
  record path's one write, a no-read delete, idempotent.

For a recorded no the same deletes run and the recorded answer returns. A
`:name-taken` leaves the cited rows alone, as every face refusal does.

**Why a missing own lock skips rather than refuses (replay).** Take the
history above, step 7 decided on the record path: the check passes, `[N1 1]`
is deleted, the answer is the recorded yes, and the worker crashes before
the ack. SPEC "What Rama showed" 2: "Records that had already completed
since the last checkpoint replay too", and "a replayed offer must get the
same answer". On the replay `[N1 1]` is gone. Were a missing own lock a
face `:no-such-lock`, the replay would answer `:no-such-lock` where the
first attempt answered yes: a violation. With the skip, every case replays
to its first answer: (i) present and matching → yes and deleted → replay
finds it missing → skipped → yes; (ii) present and differing → `:name-taken`,
nothing deleted → replay finds it present → differs → `:name-taken`; (iii)
missing on the first attempt → skipped → yes → the same on replay. The
price: a reused name with other content that cites a lock the gate cannot
deliver is answered as a retry, like a reused name after a forget; its
bytes open under no lock the store holds, so nothing is retained by it.
The plan already says "a decided name answers from its record whatever its
leases" (line 483-484); F1 keeps that sentence true.

**After F1, the lost-pool history.** Step 7: parts digest matches (lock ids
out); record yes; `R` = K0 from `[:alice :locks [N1 0]]` unwrapped with
Alice's lock; `O` = K1 from `[N1 1]`; open the resend with K1 → "hello";
`HMAC(K0, "hello")` equals the row's digest → the recorded yes, same stamp;
`[N1 1]` deleted. After a forget of the note, the same resend: `R` nil →
skipped → recorded yes → `[N1 1]` deleted → the depot's two copies (K0's
and K1's) open under no lock the store holds. P6 widened holds in all three
places.

### Trace 2. Replay — PASS after F1

- **The lease act resent and replayed.** Lease `N1`, count 4; ack lost;
  resent verbatim. Record path (record first, [F4]): parts digest matches
  (the `:lease` control value `{:count 4}` is in it; F1 removes only value
  facts' `:sealed` and `:lock-id`); a lease has no value facts, so no value
  check; the ack's `:lock-ids` are `(lease-ids N1 4)`, a function of name
  and count (plan lines 346-349, step 10). Same ids, no writes. A resend
  with count 8 → parts digest differs → `:name-taken`. PASS.
- **A crash during the gate event after the lease rows were consumed.**
  SPEC "What Rama showed" 1: an event's writes "commit together at the next
  partitioner or at the end of the event, or are discarded together". The
  consumption is in the decision's group (plan line 463, step 9), before the
  only partitioner, the person fan-out's `(|all)` (step 11). Crash at
  `:after-writes` (inside the event): all discarded; the replay finds the
  rows, decides fresh with fresh nonces for the re-wrap, and the first
  attempt's answer was never sent. Crash after the commit (in the fan-out,
  or after the event): the replay takes the record path; F1's record path
  deletes rows already gone (no-op) and returns the recorded answer; the
  fan-out re-runs its `termval` of the entry as it stands (idempotent).
  PASS.
- **A crash after a refusal destroyed leases.** Recorded no with the
  deletes committed together; the replay's record path returns the
  recorded no, its deletes are no-ops. PASS.
- **`:no-such-lock` under replay.** Fresh path: an id delivers nil only if
  never leased, leased to another session, consumed, closed, or its
  `:under` person forgotten. A replay of a face `:no-such-lock` finds the
  same absence unless a later record created the row, which only the lease
  act with that name can do; the door never cites an id before its lease is
  answered (plan 493-500). Record path: F1's skip rule (above). `:all-after`
  (P11) replaying later records first: a later `:session-closed` may delete
  rows an undecided earlier offer cites, so its replay answers
  `:no-such-lock` where no answer was ever given; the door's rule "closes
  only after every offer citing those leases is answered" (door step 5)
  excludes it for an honest door. PASS.

### Trace 3. The base and the root actor — FAIL, fixed by F2

**Source.** Default 6 (main RIG.md): "one-owner on the stream gate, owned by
the root actor, re-classed to shared at the first group; its lock rules are
shared-layer rules from day one (7b as written, locks in the record). This
overrides SPEC.md phase 3's placement of the base." CONCLUSION R8: "The root
actor has no person lock, so there's no owner-required wrap." The model's
`:base` has no `:owner` (model.clj 44-52); README 301: "A group or the base
contributes no owner subject".

**History.** The operator makes `:base` (`:kind :base`, owner the root
actor). Alice, through her session `:s2` and a permission covering the base,
leases 4 locks into `:base`, then offers a note about no one and a mention
of Bob.

**What the plan as validated does.** Line 419: "group layers and the base
are stage 3's", against default 6. Line 640: "a making act naming an owner
with no person lock is refused `:no-such-person`": the base's making act is
refused, since the root actor has no person lock. Were it made, the lease
act checks "the owner's person lock must be live" (line 632) and L23 seals
a lease row under "the layer's owner in a one-owner layer": the root actor
again, so every lease into the base is refused `:no-such-person`. And the
wrap table's "unmarked, the layer has an owner: required = `[owner]`" would
require the root actor. No value can enter the base.

**What wraps a lease row in the base, and what the re-wrap does there
(after F2).** A lease row is sealed under the session owner's person lock,
where the session owner is the lease act's `:who` when it is a person (R1:
"wrapped under the session owner's lock"). In a person's own layers this is
the owner, as L23 had it, since every session writing there in the rig is
the owner's; in the base it is the writer, Alice. A lease by the operator
(the root actor, no person lock) is stored bare, `:under nil`: nothing a
person forget could cut, and consumed at decision like any row. At
decision the re-wrap takes K from under Alice's lock and wraps it by the
table's no-owner row, because the base's owner is not a person: the note
about no one gets an empty wrap (K bare in the record's `:lock`), the
mention of Bob `{:any-of [:bob]}`, a marked value `{:required (its
subjects)}`; locks in the record (default 6). So Alice's forget does not
erase the base's note (A case 3's direction, "what must stay open stays
open"), which it would without the re-wrap; Bob's forget erases the mention
(A case 2's direction). The root actor contributes no subject, is never
looked up in `$$persons`, and the base's making act is not refused for it.

### Trace 4. The model's A cases, both directions — PASS at the lock level; end to end after F2

The eight A cases (scenarios.clj 25-54) use `:alice` (personal) and
`:group` (shared); none uses a hand, agent or base layer.

| Case | Wrap by the plan's table | Forget | Plan's result | Model |
|---|---|---|---|---|
| 1 note + mention of Bob in `:alice` | `{:required [:alice]}` both | Alice | both closed | erased ✓ |
| 2 mention of Bob in `:group` | `{:any-of [:bob]}` | Bob | closed | erased ✓ |
| 3 note about no one in `:group` | empty, K bare | Bob, Alice | open | open ✓ |
| 4 mention of Alice and Bob | `{:any-of [:alice :bob]}` | Alice | open | open ✓ |
| 5 same | same | Bob | open | open ✓ |
| 6 same | same | Bob, Alice | closed | erased ✓ |
| 7 same, marked | `{:required [:alice :bob]}` | Bob | closed | erased ✓ |
| 8 mention of Bob in `:alice` (7b) | `{:required [:alice]}` | Bob | open | open ✓ |

Both directions hold under `wrap-closed` (plan 196-197). The plan tests all
eight at the lock level (`wrap-of`, "Namespaces and tests" 1) and cases 1
and 8 end to end in `:alice` with a lease before each offer (revision item
20). Cases 2 to 7 need a stream-gated layer under shared rules; F2 gives
one, the base, so F2 adds them end to end there (CONCLUSION: "a layer gated
by the stream gate with its locks in the record" is a new model case). The
re-wrap is what makes cases 3 to 5 stay open: the lease row is under the
writer's lock, and the value must not die with the writer.

### Trace 5. P6 widened, the three places — PASS after F1

- **The depot holds only sealed bytes and lock ids.** Door steps 3 and 4
  send `:sealed` and `:lock-id` for value facts; control facts and retracts
  carry no value (D2, P13). The carried `:subjects` are person ids, not
  values. PASS.
- **No lease row survives a decision.** Admitted and recorded no: deleted
  in the decision's group (463-468). Answered from the record: after F1,
  deleted on the record path (before F1, true only when the resend cited
  the recorded ids). Face refusals keep them, bounded by the close; an
  honest door produces face refusals only as `:no-such-lock`, which by
  definition names no row. PASS after F1.
- **The lock store holds nothing that opens a forgotten value.** Value
  forget: the row deleted (personal, hand, `:own-row`) or the record's
  `:lock` excised (agent, the base); the value digest is keyed by the dead
  lock, so it confirms nothing. Person forget: the person lock destroyed on
  every task by the fan-out; the lock record stays and unwraps with no live
  person lock; the forgotten person's unconsumed lease rows are sealed under
  the destroyed lock. **Per-act grain:** one K per act, one union wrap
  (L6); a value forget erases the act's lock, so the act's other values go
  too: the model's `lock-for` gives per-act grain `[:act name p]` and
  `apply-control`'s `:forget` erases by lock id (model.clj 371-389,
  471-480), the same outcome. **Marks:** `:die-with-any` makes every subject
  required; `:own-row` puts an agent layer's lock in a row. **A person
  forget is a fact** (an operator act in `:people`); the model's
  `op-forget-person` is not (model.clj 933-941), a difference phase 8
  reports. **Time travel** shows `{:erased-at date}` and, for a value
  admitted after T, `:after-moment` (plan 551-557); the model's
  `read-as-of` gives an erased value only its erasure stamp (model.clj
  1001-1021). **Agent layers:** excision nils the row's `:lock`; the row's
  `:sealed` and `:digest` stay, both dead with the lock. PASS.

### Trace 6. The refusal order, and what phase 1 tests change — FAIL, fixed by F4

- **The model's order** (model.clj 404-452): fact outside the act's layer,
  class, the four permission checks, stale replaces, the two source checks.
  It has no value-shape, subject-count or lock reasons, and records every
  refusal (`decide`, 505-521). The plan keeps stage 1's recorded list first
  and puts `:does-not-open`, `:malformed-value`, `:value-shape`,
  `:too-many-subjects`, `:grain-mismatch`, `:no-such-person`,
  `:person-forgotten`, `:person-already-made`, `:no-such-value` after it
  (step 8), so every model reason still wins where the model has one. PASS
  on the order.
- **Stage 1 today** (gatherer, gate.clj and envelope.clj): value-domain
  checks (nesting over 32, lists, ratios, unreadable keywords) and more
  than 256 carried subjects are face `:malformed` at parse (envelope.clj
  286, 320); no `:value-shape` or `:too-many-subjects` keyword exists. The
  carried-subjects bound is on a plaintext part and stays on the face; only
  the union with the grammar's subjects moves behind the delivery, because
  the grammar reads the opened value.
- **What the plan says changes** (lines 1809-1813): malformed values now
  `:malformed-value`, recorded; unknown-layer value acts now `:no-such-lock`.
- **What also changes, not listed (F4):**
  1. stream_gate_test.clj 496-515, rows 505-508: value-domain values in a
     value fact are, unsealed, `:not-sealed` on the face; sealed, recorded
     `:malformed-value` with a stamp; the table's "none of them was
     recorded under the name" (515) no longer holds for those rows. Rows
     509 (257 carried subjects) and 510 stay.
  2. envelope_test.clj `parse-bounds` 288 and 310-314: the value-type and
     nesting rows on a value fact's `:v` leave the parser; they stay for
     control values and move to `locks/read-values` for sealed ones.
  3. envelope_test.clj `digest-properties` 416-444: "offers whose parsed
     content differs have different digests" and "every part is covered"
     fail for a value-only change (and, after F1, a lock id change); the
     parts digest covers neither; the value digest is tested instead.
  4. stream_gate_test.clj 241-242: `c/lookup` with other value content
     returned `:name-taken`; the client's `lookup` compares the parts
     digest only (plan 450-451), so value-only other content now looks like
     the record; only the gate's resend check tells.
  5. stream_gate_test.clj 303-306, "a refused first use still holds its
     name": other value content under a refused name now gets the recorded
     no, not `:name-taken`, because a refused act keeps no value digest
     (its locks are destroyed); other non-value parts still get
     `:name-taken`. A semantic change from stage 1; for Sid below.
  6. stream_gate_test.clj 216-219 and 536-537: outcomes stand; the
     `:name-taken` now comes from the gate's value check, so the tests need
     the sealing harness.

### Trace 7. No throw, and the delivery seam — no throw PASS; seam FAIL, fixed by F3

- **No throw.** Plan 1039-1046: every `rig.store.locks` function catches
  `Throwable` and returns nil (`open` on a tampered blob, where AES-GCM
  throws `AEADBadTagException`; `unwrap`, `unlease`, `deliver-lock>`); the
  canonical decode and value checks run inside `read-values`'s catch,
  returning `:malformed-value`; test 5 feeds garbage. SPEC "What Rama
  showed" 3 holds. PASS.
- **The seam.** `deliver-lock>`'s signature serves both bodies (the holder
  body ignores `*persons`). But the holder body "reads the holder, removes
  the entry, and returns K" (plan 400-401): removal at delivery. An act
  citing two locks whose second is missing is refused `:no-such-lock` on
  the face with "nothing consumed" (490-492), yet the first lock's holder
  entry is gone, so the resend the face refusal exists to allow is refused
  again. And a TaskGlobal removal is not discarded with a discarded event
  (SPEC item 1 covers PState writes), so a crash between the delivery and
  the commit loses the lock. Under the lease body the same seam is right
  only because consumption is a separate `NONE>` in the commit. **F3:**
  two functions, `deliver-lock>` (read-only under every body) and
  `consume-locks> [*layer *session *lock-ids]` (the one call site in the
  decision's group and on F1's record path; lease body: the `NONE>`s;
  holder body: the removals, after the decision). The holder body's crash
  window then narrows to a removal made just before a discarded commit,
  which the holder road already prices ("empty after a worker restart").

### Trace 8. Cost per offer — PASS; recomputed

Proportions sum to 1.000. Weighted seeks recomputed from the rows as totals:
0.568×6 + 0.25×7 + 0.10×4 + 0.04×4 + 0.01×0 + 0.002×4 + 0.015×6 + 0.005×4
+ 0.01×5 = 3.408 + 1.75 + 0.40 + 0.16 + 0 + 0.008 + 0.09 + 0.02 + 0.05 =
**5.886**, at N = 1, 16 and 128: every read is on the layer's home, so the
totals are flat in N. What grows with N is the person fan-out, 0.005·N
no-read writes per offer (0.64 at 128), costed against the one-task
placement with a break-even at N = 186; no seek grows. F1 adds to row (c)
one lease-row seek per value fact whose cited id is not the recorded one
(a door that lost its locks) and one no-read delete per cited id: at most
0.10 × f extra seeks, 5.986 for f = 1 if every resend came from such a
door; 5.886 when doors keep their locks. `lease-locks`: 3 seeks and one
range read, at the lease act's rate (about one per 80 offers). PASS.

## The template's checks, for the revision's mechanisms

- **Query topologies.** `lease-locks`: example 1, N = 3 seeks + 64
  iterations, all meaningful; example 2, 3 seeks, the empty range is the
  answer; M varies by rows and is dynamic by emission. After F2 the persons
  read is the rows' `:under` person (one per session), not the layer's
  owner: still one seek. `read-as-of` was not re-traced. PASS for
  `lease-locks`.
- **PState schemas.** `:leases` is a field of `$$layers` (same key and
  partitioner, the merge rule, L5/L22); `$$persons` differs in key (person)
  and partitioner (every task). Lease row `{:under :sealed}` is one
  fixed-keys shape; `:under` nullable after F2 (same shape). Both `:leases`
  levels subindexed; sealed slots `byte/1`, no `Object`. PASS.
- **Partitioning.** `hash-by :layer`, flat seeks; the `|all` of
  `$$persons` is small (one entry per person) and written only by person
  acts (0.005). PASS.
- **Topologies.** One stream gate, stage 1's reasons (ack coordination: the
  door's lease answer and every offer's answer by ack). The lease act and
  the close are acts of that gate: they need its record and its task.
  PASS.
- **Production readiness.** Concurrent doors: lease names are uuid7, rows
  keyed by session, a lock is consumed once in a single-threaded task.
  Client restart: fails before F1 (trace 1), passes after. Worker restart:
  trace 2. Non-idempotent writes in the stream topology: none added; the
  lease rows are `termval`s under ids fixed by name and count, consumption
  and closes are deletes, the fan-out is `termval`. Multi-partition: only
  the fan-out, replayed from the record. PASS after F1.
- **Internal depots:** none added. **In-memory state:** none built; the
  holder road's TaskGlobal is described, not built (F3 fixes its seam).
- **Minimality.** Simplest design constructed: the door seals under a lock
  the gate mints per offer inside the offer's own decision. Impossible: the
  lock must exist before the append, and the depot must never carry it
  (R1), so a separate act or a separate channel is required; R1 prices
  both and picks the lease. `lease-locks` is the channel the depot never
  sees; `:session-closed` bounds the unconsumed rows (default 1); the
  record-path deletes (F1) are what default 1's "answered from the record,
  it is destroyed" requires. Nothing to delete.
- **Throughput.** A lease per 64 locks is 0.01 acts per offer; one local
  seek per value fact at delivery; the alternative (holder) trades that
  seek for a network round trip per offer (R1). No cheaper design found.

## Fixes applied to the plan

- **F1.** Parts digest without `:lock-id`; the record path opens a resend
  with its own lock, recomputes under the recorded lock, skips a value
  whose recorded lock or own lock does not open, and deletes every cited
  lease row under the offer's session; door step 4, "Consumption of lease
  rows", "The missing lock", gate event step 3, cost row (c) and L26
  follow.
- **F2.** The base on this stage's stream gate (default 6): its owner, the
  root actor, is not a person; the wrap table's owner rows apply only to a
  person owner; lease rows sealed under the lease act's `:who` person
  (`:under nil`, bare, for the operator); the lease act checks that person;
  L23 restated; `lease-locks` unleases with the rows' `:under`; A cases 2
  to 7 end to end in the base.
- **F3.** `consume-locks>` split from `deliver-lock>`.
- **F4.** The list of phase 1 expectations that change, completed.

## For Sid (first-record picks and questions)

1. **First-record: the parts digest leaves lock ids out (F1).** The
   content check of a resend is the value digest under the recorded lock,
   as R1's rider reads; a door that lost its locks resends under new ones.
2. **The record path's skip on a missing own lock (F1).** A reused name
   with other content that cites a lock the gate cannot deliver is answered
   as a retry, as after a forget. Needed so a replay answers the same.
3. **A refused name no longer tells other value content (trace 6, item
   5).** A refused act keeps no value digest, so other value content under
   a refused name gets the recorded no. Stage 1 answered `:name-taken`.
   Keeping it would need a digest keyed by something that outlives the
   destroyed lock, which R1's rider excludes.
4. **First-record: the root actor's id and the base's owner fact (F2).**
   The plan uses the operator; R8 says "the root actor", not its id.
5. **Who seals a lease row (F2).** The lease act's person, not the layer's
   owner; the same in a person's own layers today, different in the base.
6. **A crashed door's session.** Its unconsumed lease rows stay until a
   close; the operator may close it (plan 504-506). When a session is
   closed after a crash is open item 82's.

## What this validation did not finish

The unchanged parts of the plan were not re-traced (header note). F2 adds
the base on the stream gate; stage 1's permission rule puts a permission for
the base in the base (R7, `:permission-home :in-their-layer`), and the
plan's seed grants Alice's base permission in `:alice-hand`: the build must
seed a permission in the base itself. Not traced further.

PHASE_VALIDATION:minor-fail
