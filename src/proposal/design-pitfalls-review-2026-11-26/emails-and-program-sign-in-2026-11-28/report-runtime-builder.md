# The runtime half of emails, accounts and invitations: the builder's report

The builder (Opus, high) reported in its last message; the orchestrating
session wrote it here, shortened. Commit `7bccc44` on
`auth-stores-2026-11-28`, 11 files, +348/−47: seam.clj, sign_in.clj,
main.clj, http.clj (page text only), the runtime's README, words.clj, and in
the tests README, checks.clj, proof.clj, pure_proof.clj of test-runtime and
the wire's pure_proof.clj.

## What ran (the builder's own runs)

- `clojure -M:wire -m softland.wire.pure-proof`: 40 passed, 0 failed.
- `clojure -M:runtime -m softland.runtime.pure-proof`: 28 passed, 0 failed.
- `clojure -M:runtime -m softland.runtime.command-pure-proof`: 36 passed, 0 failed.
- `softland.runtime.proof` on the in-process cluster: 58 passed, 0 failed.
- All 24 namespaces under test-runtime, `softland.command.main` and
  `softland.command.client` load in one JVM.
- Held against a broken copy: with the old way of making ids (person from
  root and UUID only, a random op per call) the proof printed 56 passed, 2
  failed ("cannot name another person who has entered", and the check of
  erasing as a knock-on).

The new checks are in `checks.clj`, run by `proof.clj` and by
`cluster_proof`; the check of a setup that comes late is in `proof.clj` only.

## Changed and left to the final run

`cluster_proof` (copy c gets `--invitation-ms 4000`), `command_proof` and
`command_review_proof` (the invite and its link), `review_faults_proof`,
`two_lines_proof`, `open_session_proof`, `worker_kill_proof` and the attacks
(all through `setup!`), `clock_ahead_proof`, `full-life-measure`.
`cluster_proof` and `command_proof` should catch a fault first.

## Names and shapes chosen

- `seam/email-hash` is the one definition; Auth's tests keep a copy at
  `softland.auth.module-harness/email-hash`.
- `:invitation-ms`, seven days, in `default-numbers` (so `--invitation-ms`).
- `derived` takes more parts after the UUID. Person id = `derived :person
  root uuid by hash`; invite op id = `derived :invite root uuid person by
  hash`.
- `:invite` gains an optional part `:herself`.
- `:take-back-invite {:claim}`, `:accounts`, `:unlink-account {:account
  [provider subject] :link}`: a person, in a session.
- Wire refusals: `:herself-is-no-yes`, `:account-has-no-form`,
  `:account-not-named`, `:link-not-named`, `:link-is-no-uuid`;
  `words/account?`.
- Runtime answers: `:do-that-at-a-browser` (403), `:find-the-invitation`
  (404, the same for "no invitation" and "not yours").
- `setup!` returns `:ends`, read back with `invitation-of`; `main` prints it
  and what to do when she comes late. The not-invited page also names an
  invitation that is over or was taken back.

## Departures

- "Nobody may name another person who has entered" is held by making the
  person's id from the claim, not by a read before the invite: a retry has
  to get the answer of its first try, which a read-then-refuse could not
  give. It leans on Auth keeping each invite's answer under its op.
- `{:refused :linking}` reaches the caller as `{:could-not :take-that :why
  :linking}`, the envelope of every refusal of Auth.
- No command was added to the command line: each would cost a function too.

## Seen and not changed

- Before she enters, an inviter can add a second waiting invitation for the
  same person under the same claim only.
- Setup run again with her id after she has entered lets another account
  with her address in as her until the new end (said in the docs).
- Setup run without `--person` after an invitation is over makes a second
  person; the first id keeps a base permission that nobody will use.
- `:invite` itself is not restricted to callers in a session.
