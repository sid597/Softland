# The Auth half of emails, accounts and invitations: the builder's report

The builder (Opus, high) could not write this file itself, so the
orchestrating session wrote it from the builder's last message, shortened.
Commit `06494ca` on `auth-stores-2026-11-28`, 14 files, +2425/−855.

## What ran (the builder's own runs, in the cloud container)

- `clojure -M:auth -m softland.auth.reference-proof 42`: 230 passed, 0 failed.
- `module-check 42 1000` on the reference (pure): 48 passed, 0 failed. Every
  broken copy is caught but `:says-of-every-enter`, which no run through the
  contract can catch.
- `module-check 42 100 softland.auth.module-harness/launch-module` on a
  cluster: 3 passed, 0 failed.
- `module-harness reruns` (the steps called by hand, no cluster): 10 passed;
  each of five broken steps fails its own sentence (no round fails D; no
  release fails D's second order; a forgotten op fails E(i) and E(ii); an
  early enter fails A; unlink by person alone fails F(i)). The script of the
  broken steps was scratch and is not committed.
- `module-harness invitations 10` on a cluster: 9 passed.
- `module-harness step3 2`: 18 batches, 0 failures; `says`, `not-reached`;
  `unfit-record`: 2 passed.
- Not run, inputs changed: `full-life-measure`, harness mode `slice-b`.

The new broken copies of the reference: `:person-has-one-claim`,
`:one-account-a-person`, `:over-invitation-lets-in`, `:early-enter-lets-in`,
`:over-invitation-stands`, `:invite-op-forgotten`, `:take-back-of-any`,
`:take-back-takes-links`, `:unlink-keeps-the-claim`, `:unlink-of-any-link`,
`:used-claim-names-another`.

## Where it departed from `emails-settled.md`

- The probe's recheck of the code claim now fires when the email's claim has
  a record that this op cannot use; its old trigger (a claim used for good)
  no longer exists and the settled file said nothing of it.
- `accounts-of` is a fourth query topology, not a foreign select (her
  entries sit under a hashed key with text keys).
- `Invitation` is a record where the settled file had a vector.
- `take-back` does not check `:person` (QUESTIONS.md 38); U1 finishes a mark
  with the mark's person and gives back the event's person.

## Beyond the ask

- module.clj: every enter step ends through `write-act!` (writes an act's
  pairs, a nil value removes its key) and `hop-or-done`; reads and writes of
  each step as before.
- schema.clj: `act-writes`, `act-op-v`. The harness: `email-hash`, `adapted`.
- `module_check` counts links made, links removed, invitations taken back.
- QUESTIONS.md: 3 turned, 35 to 40 new.

## Seen and not changed

- A release (D's second order) removes her entry and the claim's link but
  not a root that the late step 4 made: she is then "entered" with no
  account, and her next enter gets the refused enter's lane and `:making`.
- `InviteOpDone` is a memory of a call, against Sid's rule of 22 November
  (QUESTIONS.md 35).
- A refused enter is not shown linearizable now that claims can come back.
- `:linking` and every order of running again are held only by the steps
  called by hand.
- Nothing lists a person's invitations; nothing sweeps the answers of
  invites or the invitations that are over.
- `agreement/`, `PLAN.md` and `BUILD.md` still describe the old invite.

## For the builder of the runtime

`:link` is the linking enter's op, made inside the client.
`{:refused :linking}` can reach the caller. The invite's op-id is fresh for
each call and the same on each retry of one call. The client's forms are in
client.clj and in SPEC.md's contract.
