> A note of the handoff (`../HANDOFF-stored-forms-2026-11-28.md`, sections 2 and 4), written after this brief: a builder on Opus started on this brief and was stopped. The branch now has three commits more: `18b605cf` (the door, module and read-path proofs pass, by its message), `620cf968` (the seam sends a series through `door/send-acts`) and `4ad925e5` (WIP: the fixture, the judge and three pages, unverified). It gave no report. Read those three diffs first; items 1 and 2 below are done by commit message only, and items 3, 4 and 6 are begun.

You are finishing one change to the form of an act in Softland's store. A builder before you brought the source to the new form and stopped; you carry on from its branch. An orchestrating session holds the whole and has settled the design: build what is written here, prove it, commit it, and report. Where the code shows that a settled point is wrong, say so with the grounds and stop that part; do not build around it and do not settle a design question yourself.

## Where you work

- Worktree: /mnt/data/projects/Softland-act-form, branch `act-form-2026-11-28`, four commits on `main` at de68997d, working tree clean. Read `git log --oneline de68997d..HEAD` and each commit's diff first. Work only there. Never edit, stage or commit in /mnt/data/projects/Softland itself: other sessions work in that checkout.
- Another builder changes Auth and the runtime's sign-in on another branch, and your branch goes on top of theirs later. You both touch src-runtime/softland/runtime/sign_in.clj, seam.clj and src-command/softland/command/main.clj: keep your edits in those three small and local, and do not reformat them. Do not touch src-auth, test-auth or Auth's SPEC.md.

## Rules of this repository that bind you (you load no memory, so they are here)

- Read first: CLAUDE.md at the root; .claude/output-styles/build.md; src-store/CLAUDE.md, which says what is written beside the store's code (code is the truth of what happens, a check with its sentence is the truth of what must hold, a docstring does not retell a body, a note that a change makes false is changed in the same commit, what Sid decided lives in the "Decisions of …" sections of src-store/README.md with his lines as typed). Load the Rama skill (.claude/skills/rama/SKILL.md) and rama-pitfalls (.claude/skills/rama-pitfalls/) before you read or write Rama code.
- Never read src/app/server/env.clj or any .env file.
- Commit on the branch by concern, each commit one change, with a message that says what and why, in the plain style of `git log --oneline -15`. No Co-Authored-By, no session link, no attribution line of any kind. Stage paths by name, never `git add -A`. Do not push, open a pull request or merge. The last commit on the branch begins "WIP:"; leave it as it is and commit on top (do not rewrite history).
- Delete no file but scratch that you made. Keep nothing you want kept under /tmp.
- Proofs on a cluster run one at a time on this machine, behind one lock: `flock /mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock clojure -M:… -m …`. Another builder runs proofs at the same time, so a wait for the lock can be long: run a proof in the background and wait for it, do not poll. Pure proofs need no lock. The commands are in deps.edn's comments and in test-store/softland/store/README.md.
- The Bash tool's shell is zsh: an unquoted `$var` is not split into words; write a loop over commands as `bash -c '…'`. A proof's JVM loads source when it starts: make no source edit while a sequence of proofs still has one to start. Never open a file under a `runs/` folder whose name begins `raw-`.

## What the change is

Sid said yes to these on 28 November; the shapes and names inside are a session's and his to turn.

- F23: a fact's key may be an id (a readable keyword or a UUID) as well as a keyword.
- F24: `:subjects` and `:claimed-when` leave the act.
- F22: the depot word leaves the act's id. An act's id is `[lane uuid]`; a fact's id is `[[lane uuid] index]`. A lane's depot is found in the lane's own fact, `[lane :softland.store/depot]`; where a lane starts stays a pure function of its id (`first-act/depot-at-making`: the base on the microbatch depot, every other lane on streaming, the default). Changing a lane's speed later is not built; nothing in the stored form may stand in its way.
- F2, the store's part: the entries of src-store/README.md that say "Sid decided" without a line of his are marked "a session's default, Sid's to turn". No rule changes.

The act's form as the source now has it (act.clj):

```clojure
{:act
 [:parts
  [:version      [:enum version]                    {:reason :unknown-version :edition true}]
  [:who          :author-party                      {:reason :who-not-a-party}]
  [:lane        :id]
  [:permission   :uuid                              {:optional true :reason :permission-not-an-id}]
  [:session      :id                                {:absent :no-session}]
  [:based-on     [:map-of :fact-id :carried-stamp]  {:optional true :default {}}]
  [:because-of   :act-id                            {:optional true}]
  [:facts        [:seq-of :fact]                    {:empty :empty-act}]]
 :fact
 [:parts
  [:e        :id]
  [:k        :id]
  [:v        :value   {:optional true}]
  [:replaces :fact-id {:optional true}]]
 :act-id  [:tuple :id :uuid]
 :fact-id [:tuple :act-id [:int {:min 0}]]}
```

What arrives on a depot is `:act-id`, `:door-time`, then those eight parts.

## Where the builder before you left it

Commits: `34a88494` (F24), `567018b8` (F23), `ea6cbb06` (F22, source and pure proofs), `37a5be43` (WIP: F22 in the cluster proofs, edited and loading, never run).

Decisions already taken; do not take them a second time another way:

- `:depot` left the appended thing altogether: the door adds `:act-id` and `:door-time`. A thing carrying `:depot` is refused `:unknown-part`.
- `first-act/depot-as-said [lane said]`: the depot word that the lane's depot fact says, else `depot-at-making`. The door and the answer query both use it.
- `act-id-for` and `act-ids-for` keep their signatures and make no round trip. `door/send-acts [door pairs]` is new: a sequence of `[act act-id]`, one depot read per lane for the series, stops at the first word.
- `stray-reason` takes one argument; the stray word `:act-id-of-the-other-gate`, the rule `:depot-is-the-act-ids-depot` and the reason `:depot-not-the-act-ids-depot` are gone.
- A thing on the other gate's depot is kept as it was: refused `:lane-belongs-to-the-other-gate` in that gate's own answers, with a stamp of that gate's own count for the lane. Leave this rule as it is; it is on Sid's list.
- `record/answer-read` gives the key alone; `written-by?` is `[lane row fact-id]`. The `:key` leaf is gone; `in-store-namespace?` guards with `keyword?`.
- F24: none of the four writers adds a `:subjects` fact: the making act and the two gives name her as `:holder` of the permission they write, and the name act has her as `:e`. Leave it so.
- In the proofs: `arrived`, `arrived-making`, `acts-in-order` and `out-of-their-line` lost their depot argument; new helpers `by-hand!`, `gates-answer`, `gates-last-stamp` (door_proof) and `as-the-gate-kept` (module_proof).

Pure proofs passed at `ea6cbb06`: store `act-proof` 157, `pure-proof` 779, `permission-proof` 72, `read-path-pure-proof` 289; wire `pure-proof` 39; runtime `pure-proof` 25; `command-pure-proof` 35.

## What is left, in this order

1. **Run the store's cluster proofs and mend what they show**: `door-proof`, `module-proof`, `read-path-proof`. The builder expected faults of counts or wording in the groups it rewrote: door_proof `round-trips-are-counted`, `the-base-through-the-door`, `the-depot-of-a-lane`, `a-thing-on-the-depot-of-the-other-gate`, `reads-that-throw`, `two-sends-into-the-base-are-decided-in-their-order`; module_proof `strays-and-limits`, `each-gate-tells-the-other-gates-lanes`; two module_proof sentences now expect `:unknown-part` for an act carrying `:claimed-when`; read_path_proof's new `a-key-that-is-a-uuid`. Before you change a sentence to make it pass, decide whether the source or the sentence is wrong, and say which in the commit. One sentence of `read-path-proof` is known to fail about one run in three on `main` itself ("through three lanes, a permission is given in the nearest …"): if only that one fails, run once more and move on; do not mend it.
2. **The seam's series.** `send-all-now` in seam.clj sends each act of a series with a depot read of its own (2n round trips for n acts; it was 1 + n). Bring it onto `door/send-acts`, so that a series pays one read, if that keeps what `send-all-now` does for each act (its tests of the act, its ration, stopping at the first word); if it does not fit in a small change, leave it and say why.
3. **The run-again fixture.** Remake it once, the way run_again_fixture/README.md and `run_again_proof.clj`'s `make-fixture` mode say, say in the fixture's README what remade it, and run `run-again-proof`.
4. **The judge**, src/proposal/runtime-2026-11-21/gate-histories/: histories.clj lines 76 and 927 and fix.clj lines 134 and 235 hold `[lane :streaming uuid]`; record.clj, walk.clj, today.clj and piece_2 hold it too. Bring to the new form what the judge's `fix` mode runs (`clojure -M:run fix 2000 1` in that folder; check its README), run it once, and list what you left.
5. **Digests written down as constants**: search the proofs, the pages and the judge for 64-hex constants that are a digest of an act or a thing; bring each to the new form by running, not by hand.
6. **The pages.** src-store/MODEL.md line 36; src-store/softland/store/door/README.md (near lines 32, 60–61, 122, 147–162, 221–223, 245–265); src-store/softland/store/README.md (near 552, 609, 1403–1412, 1437); test-store/softland/store/README.md (near 330, 340–343). src-server/softland/server/sign_in.clj still writes `:subjects`: fix it if the server's `pure-proof` (`-M:server`) loads it. In src-store/README.md's new section "Decisions of 28 November 2026 — the form of an act", add entry 3 for the depot word, with his lines as typed: asked whether a lane's speed should be changeable after it is made, he answered "4c yes should be controllable"; and of 7 October, "members choose yes, default is streaming yes". Say under it, as a session's and not his: where the depot is found, that a maker choosing another speed is the same step as changing it later and is not built, and what a change of speed would still have to build (the read path still picks tables by `depot-at-making` in sources.clj, acts.clj, lanes.clj; a thing that was appended to the other gate's depot leaves a refusal under the same answer key in that gate's record).
7. **F2.** In src-store/README.md mark as "a session's default, Sid's to turn" the sentences that say he decided without a line of his: 6 October entries 4 and 5, and 13 October entry 6. src/proposal/design-pitfalls-review-2026-11-26/run-5-pages-that-say-decided.md says which sentences (break 4, and its inventory line for 13 October entry 6). Mark those and no others, keep every quoted line of his as typed, change no rule. Mark also where 6 October entry 5 and 7 October entry 10 describe the old id, pointing to the new entry.

## What you run

By Sid's word, only what holds the thing you changed; everything else is run once at the end, on all the branches stacked, by another runner. So: the store's four pure proofs and the wire's, the runtime's and the command line's pure proofs after your last source edit; `door-proof`, `module-proof`, `read-path-proof`, `run-again-proof` and the judge's `fix` once each when you think them whole; the server's `pure-proof` if you touched src-server. Not the runtime's cluster proofs, the command proofs, the attacks or the measures.

## Your report, as your last message

Read by the orchestrator, into a handoff page. Plain words, no preamble, short:

1. The commits, each with its hash and what it holds.
2. For each of the seven items: done, or what is left and why.
3. What ran, with counts. For each sentence you changed to make it pass: which, and whether the source or the sentence was wrong.
4. Every change beyond the seven items, from `git diff --stat 37a5be43..HEAD`.
5. Any decision you had to take that this page did not settle, each in a line, with the other option.
6. What a send costs now in round trips: one act, and a series of n through the seam.
7. Which lines of sign_in.clj, seam.clj and main.clj the branch changes against de68997d.
8. What is known to fail, and which proofs of the full run should catch a fault of this branch.
