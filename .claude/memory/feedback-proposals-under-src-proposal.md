---
name: feedback-proposals-under-src-proposal
description: "Where working material goes — live handoffs and proposals under src/proposal/<work>*; history/ only when Sid names it (retiring superseded material); big fetched captures in the depot outside the repo, never in git"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 79987ba3-fd41-463e-991b-51bf230ca76b
  modified: 2026-09-21T14:15:50.465Z
---

Session handoffs, fact bases, starters and proposals for a piece of work go
under `src/proposal/<work>*` (for example
`src/proposal/inland-integration-2026-09-14/`). `history/` is the archive of
earlier work and is out of bounds for new writes.

**Why:** Sid, 2026-09-14, after a fact base and starter were placed under
`history/docs/`: "nope not in history ... history is out of bounds add them to
src/proposal/<work>*". The archive keeps records of what happened; a proposal
is live material for the next session.

**How to apply:** When Sid asks to write down gathered context or a next-session
prompt, create or reuse a `src/proposal/<work>...` folder. Do not add to
`history/` unless Sid names it. Related: [[project-inland-integration-2026-09-14]].

**Retiring superseded material (Sid, 2026-09-21).** When one write-up replaces a
pile of earlier material, Sid wants the proposal folder to hold only what gets
passed on, and the rest moved to `history/` ("I will not tell any session to
read them so let's move them to the history folder"). He named it, so that is
inside the rule above. Shape used: `history/proposals/<work>/` with a README
mapping the folder, a row in `history/README.md`, records left in their
original wording. Nothing is deleted in such a move. See
[[project-first-record-research-application]].

**Big fetched captures stay out of git (Sid, 2026-09-21).** Saved web pages and
PDFs from research sessions (363 MB that time) go to a depot beside the repos,
`/mnt/data/projects/research/<project>/<work>/`, with a README there and a
pointer from the repo. Why: "it's too much", git cannot shrink PDFs, a commit is
permanent, and he worries about what it does to backups. No symlink from the
repo into the depot, since a backup that follows links would pull it back in.
`.gitignore` has `src/proposal/**/sources/`. A research session should fetch
into the depot in the first place.
