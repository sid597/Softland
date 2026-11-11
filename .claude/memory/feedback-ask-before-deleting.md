---
name: feedback-ask-before-deleting
description: "Before deleting any file, even a regenerable one made this session, say what and why and wait for Sid's word; tidiness is not a reason to delete (2026-09-21); not covered: scratch that holds no work, and a source file whose code was moved in the same commit (2026-11-11)"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 79987ba3-fd41-463e-991b-51bf230ca76b
  modified: 2026-09-21T14:16:04.589Z
---

On 2026-09-21, while archiving research, a session started an `rm` on 14 bundle files it had
generated itself (regenerable, unread, 0.8 MB) after announcing it in one line. Sid stopped the
call and asked: "what are you trying to remove and why are you trying to remove it". The honest
answer was that it bought tidiness with a deletion and the files were mildly useful. They were
kept.

**Why:** a one-line announcement followed by the act gives Sid no real chance to weigh in, and he
cannot tell from a list of file names what is being lost. In this project superseded material is
moved, not destroyed.

**How to apply:** treat a deletion as a question to Sid, not a step in a plan: name the files, what
they are, why they would go, what is lost, then wait for his word. Default to keeping or moving.
Being regenerable or self-made lowers the risk; it does not replace asking. Unstaging from the git
index or moving a folder is not deleting, but still say exactly what happens to the bytes on disk.
Related: [[feedback-proposals-under-src-proposal]].

**11 November 2026: two things the line does not cover.** While the guidance was being
rewritten, a session proposed "a session may delete files it created in that same session", and
Sid answered the whole list with "ok lets dothat". The line on `CLAUDE.md` is narrower than that
proposal, because the fourteen files of 21 September were also made in the session that wanted
them gone. Not covered, so no asking: scratch that holds no work (a temporary script, a
throwaway fixture, a test worktree), and a source file whose code was moved or merged in the same
commit, which the session names in its report. Anything else that holds work is still a question
to Sid, self-made or not.
