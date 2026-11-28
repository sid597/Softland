# A program that signs in: the builders' report

Two builders (Opus, high). The first was stopped by a restart of the
container after one commit and left no report; the second finished and
reported in its last message, which the orchestrating session wrote here,
shortened.

## The commits, on `program-sign-in-2026-11-28` from `0f2024f`

- `44036ec` (first builder): the yes page signs in an agent of hers again,
  and lends what she ticks in her own lane (parts 6 and 7 of the brief).
- `1fa4148`: the first builder's half-made edits of part 5, committed by the
  orchestrator as WIP after the restart.
- `769531c` (second builder): a fix to `44036ec`. Its check "The same agent
  signed in a second time…" wrote under the lend id of the new sign-in; no
  yes writes that lend, so the check would have failed. It writes under the
  first lend.
- `1062da9`: part 5 finished, no program is a person. SPEC.md (the refusals
  table, begin-sign-in, the ask list, a dated entry), the note on
  QUESTIONS.md 24, the command line's docs, and every check rewritten so
  that programs sign in as agents.
- `fff2b5a`: part 8, whose call a thing is: the marks in SPEC.md,
  QUESTIONS.md, Auth's README, reference.clj, the sentences of
  reference_proof and the docstrings of module_check.

## Beyond the four parts

- `browser-line` moved from checks.clj into drive.clj, which now requires
  the command line's client.
- drive waits and retries while the runtime paces sign-ins (a 429 page, or
  `:take-more`, for up to a minute): the checks now ask more than 30 times
  in 10 seconds from one address, and without the wait `runtime.proof` gave
  66 passed, 3 failed, all on 429.
- `drive/default-agent`, `perm/lent-at-yes`. The "looked-at" ask of
  `44036ec` goes through `drive/program`, so it is paced too.
- full_life_measure: the 500 ended sign-ins of P are `:browser`, since
  `:program` would be refused.
- The command line's README and docstring say that an agent signed in again
  with no box ticked, or one made by `agent new`, needs `--permission`.

## What ran (logs were under /home/user/proof-logs/psi/ in the container)

- Auth `reference-proof 42`: 238 passed, 0 failed. The broken copies
  `:person-begins-a-program` and `:yes-names-the-person` are caught.
- Auth module check, pure (`42 1000`): 51, 0. On a cluster with 100
  histories: 3, 0.
- Pure proofs: the wire 41, 0; the runtime 29, 0; the command line 37, 0.
- `softland.runtime.proof`: 69, 0 (all the lend checks of `44036ec` and the
  new sentences).
- `review-faults-proof` 6, 0; `attacks.yes-page` 1, 0; `command-proof` 16,
  0; `clock-ahead-proof` 4, 0; `command-review-proof` 11, 0;
  `line-review-proof` 14, 0. All 34 namespaces load.
- No new sentence of the runtime was run against the old code or a broken
  copy; only Auth's two broken copies show a new rule failing.

Changed and left to the final run: `cluster_proof`, `worker_kill_proof`,
`two_lines_proof`, `open_session_proof` (it now signs in an agent by
default), and the attacks `closed_session`, `slow_readers`,
`unwatched_standing`, `watch_caps`, `lost_replacement`, `sign_in_pace`. A
fault would show first in `two_lines` (agent-to-agent lends) and
`slow_readers` (`:under` at her browser).

## Choices (the other option in brackets)

- The refusal word is `:is-a-person`, after `:no-such-actor` [a new word].
- `person?` needs actor = root and kind `:browser`.
- `:do-that-at-a-browser` left the wire; its case is held by `person?`
  [keep it as a second word]. A program she signed in that says a person's
  word is answered `:do-that-as-an-agent`.
- `drive/yes {}` defaults to a new agent called "a program" with both boxes
  ticked [a name at every call].
- The check of the person's words is in checks.clj: every `:a-person` word
  of the table, and `:invite :herself`, read from the table itself. The
  command proof checks `invite`, `under` and `end-sign-in` as processes,
  exit 1.
- The pure proof adds: her root on a program's sign-in is answered as an
  agent at every word.
- `review-faults` case 2 is "a new agent, then one of hers".

## The yes page as it reads

- "As a new agent, called [name]", with "to see" and "to write" both
  ticked, and a button "Yes, as a new agent".
- If she has live agents: a list of them by name with radios, then "It
  keeps what it holds. It is lent besides…", both boxes unticked, and a
  button "Yes, as this agent". If she has none, a line says so.
- "No".
- After a yes: with a box ticked and the lend taken, 200, "signed in as X,
  and it is lent to see/write…". With a box ticked and the lend failed, 503,
  "signed in… but what you ticked could not be lent: it holds nothing of
  it. Send this page again…". With nothing ticked, 200, "holds nothing" for
  a new agent or "keeps what it holds" for one of hers.
- A post as herself gets 400. A post naming her own id as an agent gets
  409, "never signed in as you". A second answer gets 409, "signed in as an
  agent of yours, NAME".

## Sentences whose meaning changed: 14

First to look at: the yes's actor "is she" became "is an agent of hers and
not she"; "same word from a program that holds her sign-in" became "…a
program she signed in is refused"; the command proof's `invite` and `under`
became refusals; review-faults and attacks/yes-page, "signed in as her"
became "as an agent of hers". The rest: `git show 1062da9`.

## Seen and not changed

- Run 5's break 4 is in the store's README "Decisions of" section, not in
  SPEC.md; marking it is beyond Sid's yes to part 8, so it is left.
- A command line signed in again with no box ticked has no default
  permission that works; it needs `--permission`.
- None of the person-only commands reads worse than a plain refusal.
- The long lines that part 8's replacement left are not reflowed.
