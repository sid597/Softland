# Starter: finish the read build — four repairs, the review of the whole, the measures, the receipt

Cut by: the build session "read implementation" (Opus 5.5), 12 October 2026, 16:20 IST, at Sid's word
"tell me the prompt needed i will start manually".
For: one session that Sid starts by hand, in a mode in which it cannot be asked for a permission.
That matters more than anything else on this page: an agent that waits for a permission sends its
parent nothing, and the last two builds stood still for about three hours that way.

**The goal.** When this session ends, the read build is done: a live read is told each change at
once and not a second late; a live read that is too large is turned away at once; the whole build is
reviewed against the contract and what the review found is fixed; the six proofs pass and the
measures are run on the final tree; everything is committed; and Sid has one receipt.

**How much there is to read, and so how the session works.** The session itself reads this file, the
build's page, the contract, Sid's decisions and three pages of the skill: about 150 KB. The whole
that the review audits is far more: 553 KB of store code in 26 files, 881 KB of proofs in 7 files,
250 KB of maps, 146 KB of build papers. One context cannot hold that whole. So the session gives the
two heavy pieces each to one fresh agent (the Agent tool, model Opus 5.5, effort xhigh), one after
the other and never side by side, since both change the same files: first the four repairs of
section 1 with their proofs; then the review of the whole of section 2, which also audits the
repairs. Each agent is given this file, the build's page and the contract to read, the section that
is its own, and the rules at the end. The session takes each verdict from the agent's paper, as Sid
said on 12 October: a build is not judged a second time. It runs sections 3 to 5 itself.

**Never open a raw file.** Nothing reads, greps or prints a file under `runs/` whose name begins
`raw-`. One of them, `runs/raw-rp-measures-full.txt`, is 1.0 GB of Rama's stack traces: about 260
million tokens, which no context holds. What it had to say is in `runs/measures-read-path.txt` and in
ruling 91. A search of this folder names its files, and never uses `-r` on `runs/`.

## Where the build stands

All three parts of the read build are planned, checked, built and committed. Nothing is running. The
head is the commit that holds this file. The parts: `371c6d2f` what the gates keep; `fd92e6e9` the
base for a reader; `16ced230` the read path. The six proofs passed in the last build's own final
run, each with exit 0: act 160, pure 686, module 354, door 199, the read path's pure proof 149, the
read path's proof on the cluster 445. `test-store/softland/store/README.md` gives each command.

It is not done. Two faults that the measures found are still in the tree, the skill's last stage has
not run, and there are no final measures and no receipt.

## Read first, in this order

1. `CLAUDE.md` at the root, and `src-store/CLAUDE.md`: the explanation rules bind every file.
2. The Rama skill (`/rama`): `SKILL.md`, `references/phases.md`,
   `references/phase-full-spec-review.md`. Load it before any Rama code is read or written.
3. `../BUILD-reads.md`, whole: the contract that the review audits against. Sid's decisions in
   `src-store/README.md` win over it; the section of 11 and 12 October is this build's own.
4. `PROGRESS.md` in this folder, whole. Rulings 1 to 90 are the build session's and bind you. A
   ruling is not a fault; if one cannot hold, say so at the top of your paper with the evidence, and
   do not undo it. Its last section says where each part of the receipt is.
5. The papers of the three builds in this folder: `IMPLEMENTATION_VALIDATION-*.md` and
   `TEST_VALIDATION-*.md`; `runs/measures-read-path.txt`; `runs/raw-rp-probe-bytes.txt`. The plans
   and the implied spec where a question needs them.
6. The code and the proofs, whole: `src-store/softland/store/` and `test-store/softland/store/`,
   with every README of both trees.

## 1. The four repairs, already ruled

Build each with its proof and its explanation. No check of a proof is removed; one that a repair
makes untrue is written again for the new truth (ruling 1).

- **Ruling 86: a fact's row holds its value as a String.** A live read of a fact's row is told a
  change about a second late, in a streaming layer too (the measures' M4: 1,000 ms, against 0.15 ms
  for a row of the facts by value). The cause was run (`runs/raw-rp-probe-bytes.txt`): Rama sends a
  push on rows that hold a byte array the whole value anew about once a second, where the same rows
  with a String get one change for each write. So `:value` becomes a String and no byte array, in
  the schema of the facts tables of both gates and wherever a fact's row is made or read. The form:
  the value's text itself when the plain bytes are well-formed UTF-8 that does not begin with the
  character U+0000, which is every value that came through the door; else U+0000 followed by the
  bytes, one character for each byte (ISO-8859-1), so that nothing is lost for bytes that are no
  text, and A1's "keeps those plain bytes" and ruling 14 still hold. One pair of functions in
  `record/rows.clj` goes from bytes to that String and back; nothing else knows the form. The digest
  of a value stays the digest of its bytes, so no key of the facts by value changes. The rows of the
  answers and of the locks keep their byte arrays. Before you rest on the form, run two things on
  the in-process cluster: that a row whose String holds U+0000 is written and read back the same by
  a stream and by a microbatch topology (if not, take another mark that no well-formed text can
  begin with, and say so); and that a push on one such row, and on a run of such rows, now gets one
  change for each write. Then run M4 again. The receipt gives both numbers, before and after.
- **Ruling 87: the read path bounds its own pushes.** Rama as shipped opens 1,000 pushes on one
  table on one task. A canvas of 5,000 kept live through three layers asked for about 10,000 and was
  turned away only after 9.3 s. The read path counts the pushes that it holds on each table and
  task, and turns away a read that would take it past its own bound, 900, at once and before it
  opens any push, with a plain word of its own. And the number that bounds the checks of ruling 12
  for one live read comes down from 40,000 to 250. Both are numbers of the build, kept and not
  decided, and are listed as such. A read asked once is not bounded by the count of pushes. Rama's
  1,000 is the default of a dynamic option, `pstate.proxy.limit` (ruling 91: read in Rama's own
  message, not run with another value). Do not set it: say in the docstring that the read path's
  bound stands under that default, and that raising it is a setting for Sid to weigh.
- **Ruling 89: a read asked once reads the table**, and takes no open push's value. One function
  switches it. The docstrings and the proof's checks say what is then true.
- **Ruling 84: two lines of `door.clj`** still say "the two depots of the module" (near lines 41 and
  696). The module has a third depot, which takes no act and which the door does not open.

## 2. The review of the whole

By `phase-full-spec-review.md`: audit the whole build and its proofs against the whole contract, fix
what you find in place, and audit again until a full pass finds nothing new. At the least:

- every line of the contract's "What to build", "The builder's rules" (R1 to R8), "Not decided"
  (each built as kept, with a docstring that says that it is not decided), "Not in this build, by
  decision" (nothing of it is built), "Explanations, as the tree's rules say", and "When this build
  passes";
- the code against its own explanations, in both directions. The three builds ran one after the
  other and partly side by side, each from a plan written before the build before it: look for a
  line that one build wrote and a later one made untrue;
- fault tolerance: an event that Rama runs again; a second attempt of a batch; a worker that starts
  again; the read path under a push that Rama ends, a value sent anew, a callback with the same
  value, a read that leaves inside its own function;
- the proofs: whether any check can pass falsely, and whether each requirement of the implied
  spec's "What the proof of this build must be able to show" has a check;
- R1 by a search of the tree; R2 by a list of what the read path holds between two calls; R3 by
  trying to make a query send Rama a path on a whole table; R6 by the lists of words.

A thing that only Sid can decide is not fixed. It goes into your paper as a row proposed for his
queue, decided by nobody. Write `FULL_SPEC_REVIEW.md` in this folder: the verdict, the four repairs
and what each changed, each fault with its evidence and its fix, each pass of the audit, what you did
not reach. Its last line is `PHASE_VALIDATION:pass` or `PHASE_VALIDATION:fail`.

## 3. The proofs and the measures on the final tree

Run the six proofs; each must pass with exit 0. Keep each one's output, cut to its `SHOWN` lines
once, its count line and its exit status, in `runs/final-<proof>.txt`. Run the measures of the
contract's section C once, by the command that the proofs' map gives, and keep what they print,
whole, in `runs/measures-final.txt`. Do not keep Rama's own log of that run: the last build left a
file of 1.0 GB that way (`runs/raw-rp-measures-full.txt`, which waits for Sid's word and is never
committed).

## 4. Commits

On `main`, by named paths in the commit itself (`git commit -m ... -- <paths>`): another session's
staged rename is in the index. Group by concern: the repairs and what the review changed, with their
proofs; then the papers and the page. Each message says what and why. No `Co-Authored-By`, no session
link, no attribution line. Never push. Bring `PROGRESS.md` up to date: the table of the stages, a
ruling for each thing that you settle, and the lists for the receipt.

## 5. The receipt to Sid

In the chat, and as `RECEIPT.md` in this folder. Its parts are the contract's list ("Commits and the
receipt"); the last section of `PROGRESS.md` says where each is. It opens with the line "proven by a
hand-played asker, used by no inhabitant yet". Sid reads the working: give each number with what was
run, and keep apart what was run, what was modelled, and what was only read.

## Rules

- Decide none of Sid's open questions. The rig, the server and Inland are examples: no form, name,
  call shape or default from their code. Never read `src/app/server/env.clj` or any `.env` file.
- Design for production. If you find yourself reaching for a kept copy of what the cluster holds in
  the memory of a process, stop and say so.
- Other sessions' files are in the tree and are not this build's: the staged rename under
  `decisions/groups/`, untracked files there and under `docs/`, a change of
  `src/app/server/README.md`. Leave them.
