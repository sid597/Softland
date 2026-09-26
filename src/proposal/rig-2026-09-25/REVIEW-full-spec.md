# Full-spec review of the rig (26 September)

The rama skill's full-spec review of the whole module against SPEC.md, the
plans, and RIG.md's rig choices, defaults and rulings. Reviewer: Claude
Opus 5.5, for builder C. Branch `rig-review-full-spec`, worktree
`/mnt/data/projects/Softland-rig-review-full-spec`, made at
`rig-2026-09-25`'s head `13c8360b` (step R landed, phase 8's pass 3 landed,
step 6b in flight in Codex).

## How this review differs from the skill's

The skill's full-spec review (`phase-full-spec-review.md`) is one session
that finds, fixes, reruns the suite and loops until a pass finds nothing.
This one finds only. C's instructions: no code changes, no fixes, no test
runs of any kind (Sid's rule, about 08:20: test only when it is needed; a
review is done by reading). So every finding below is reasoned from the
code and says so; where an existing test already exercises a path, the
test is named. By the skill's own rule the verdict is `fail`, because
items are outstanding and nothing was rerun; that verdict is the skill's
form, not a judgment that the rig is unsound.

## Scope

Everything on the branch but step 6b's path, which Sid is rebuilding in
Codex and which gets its own review: `micro/control-keys`, the micro fold's
two opens `arrival-open` and `row-wraps` (their use of the compiled
grammar), the compiled constants `grammar/grammars` and `reads/seed-hints`
on the micro side, and the micro side of the machinery count. Where a
finding touches 6b's ground it says so. Phase 7's harnesses are on
`rig-build-numbers`, not on this branch, and were not reviewed; phase 7's
plan was read only for what its workloads touch.

## What was read

- The spec and its sources, whole: SPEC.md; PROGRESS.md "Now" (lines
  9-195); the formal model's README 42-152; `model.clj`; `scenarios.clj`.
- IMPLICIT_SPEC.md sections 0 to 3, every operation and read, and the open
  list O1-O23 (the entity matrix consulted where a finding needed it).
- RIG.md whole: the Overnight state, Found tonight, For Sid 1-68, the
  defaults 1-8, rig choices R1-R97. REVIEW-wave1.md whole.
- The module source whole, every namespace under `src/rig/store/`, and
  `src/rig/revision.clj` by its entry points and its git road.
  `src/rig/claims.clj` (phase 0's check) and `src/rig/bench/lock_slice.clj`
  (phase 7's slice) were not reviewed as store code.
- The tests by targeted reads, and a read-only gatherer's map of which
  tests cover each test the spec names (Sonnet; three of its citations
  spot-checked: `stream_gate_test.clj` 494-498, `promote_test.clj`
  492-507, `forget_test.clj` 612-625). `gate_test.clj` is not in this
  worktree; nothing opened it.
- The plans and their validations by targeted reads where a finding
  needed the plan's decision.

## Severity

- **High**: a ruled path the store core would take stops working, or data
  is exposed or corrupted, on a path an ordinary store takes.
- **Medium**: a spec clause or the model's decision is broken on a narrow
  path, or a pick contradicts a ruling and has not reached Sid.
- **Low**: a gap or inconsistency with no wrong outcome today, or a
  reading that has not reached Sid.

"First-record" means the finding bears on what a kept record would carry.

## Findings

### H-1 (high; first-record): once a layer that is someone's working layer is re-classed, every read entry into it is refused, so the one exit shows that reader nothing

Reasoned from the code, not run. No test re-classes a working layer.

- **Where.** Every act the read machinery writes into the working layer is
  built for the stream gate and tagged by layer:
  - `read_exit.clj` 101 and 124: the read entry, `:class :by-layer`, named
    `(env/make-name working :by-layer)`, sent by the stream door
    (`client/offer-until-answered!`, 134);
  - `standing.clj` 36, 73, 112 and 126: every standing-read line, the same;
  - `read_exit.clj` 203 and 219: a session close with `:reads`, and each
    dropped entry's forget act, `:class :by-layer`;
  - related, on phase 6's stream side: `recipe.clj` 267 names every tool
    run `[layer :by-layer :offer id]` (T-FR5, first-record), while
    `runner.clj` 155 takes the act's class from the layer's settings.
- **What happens.** A re-class moves a one-owner layer to the micro gate,
  and the stream gate then refuses a by-layer offer into it
  `:class-mismatch` (`gate.clj` 224). `micro_test.clj` 405-420 does exactly
  this to the agent session `:alice-agent` and then writes into it at the
  micro gate, green. The exit's next entry into that layer goes to the
  stream gate on the layer's home and is refused either way:
  - if the door still holds locks it leased before the re-class, the
    entry is decided and recorded `:class-mismatch`;
  - otherwise its lease act, also `:class :by-layer`, is refused
    `:class-mismatch`, the door seals under throwaway locks
    (`client.clj` `assign!` 176-207), and the entry is refused
    `:no-such-lock` on its face.

  `read!` then returns `{:refused r :entry nm}` and shows nothing
  (`read_exit.clj` 136-142), since nothing is shown before the entry is
  acknowledged. The same holds for every standing-read line. A drop cannot
  run either: each dropped entry's forget is `:class-mismatch` at the home,
  and a close with `:reads :drop` sent to the micro gate is
  `:malformed-control`, since `micro/close-ok?` (354-359) takes only
  `{:session s}`. A tool run in that layer is refused `:mis-tagged` on its
  face (a name tagged `:by-layer` on an act of class `:by-entity`), and
  the runner's own `[:k :tool]` read is refused first.
- **Why it matters.** Default 4 records an agent's reads in its own
  session layer ("Agent session reads are recorded there, and kept or
  dropped when the session closes"), and an agent session writing
  continuously is the hot layer ruling 2's re-class exists for
  (IMPLICIT_SPEC OP7: "typically an agent session writing continuously").
  After the re-class the agent can still write into its session, at the
  micro gate, and can read nothing through the one exit. A person who
  re-classes her own layer loses her reads the same way. The plan assumed
  this could not happen: PLAN-reads-rest.md 341-344, "entries are written
  into the reader's working layer, always one-owner, FR3", and 1066, "the
  reader's working layer, a one-owner layer on the stream gate". A
  re-classed layer is one-owner in kind but placed by entity; even with
  its entries routed to the micro gate, RR4's `open-entry>` would answer
  their `:no-copy` rows `{:unreadable :no-copy}`, so the close and the
  drop would need the same repair.
- **Against the spec.** SPEC phase 3: "Re-class of a hot layer moves it
  here." Ruling 3 (PROGRESS 78-80): "a model's or a person's reads are the
  crossing's exact list, always, because there is no re-run." Default 4.
- **First-record.** Yes. FR3 ("an ordinary act in the reader's working
  layer ..., through `*offers`") and FR4 fix the entry's gate and the class
  its name carries (For Sid 3); T-FR5 fixes the run name's `:by-layer`
  tag. An entry into a re-classed working layer needs a name tagged
  `:by-entity` and the micro door, and a kept entry carries its name.
- **Phase 7.** Its workloads never re-class (PLAN-numbers.md mentions
  re-class once, at 613, quoting README), so this does not block the runs.
  It does qualify number 3's verdict: re-class is "the way out for a hot
  layer" the verdict points to (README.md 121), and today that way out
  ends the layer owner's reads when the layer is also a working layer.
