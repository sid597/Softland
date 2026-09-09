# ECS round — the asks

Three builds came out of the ECS hypothesis. This file is the read of them:
six parallel sessions over the four write-ups, one body, three asks, both
families; then a merge session over the four docs and the six outputs. The
meta prompt (the loop itself, what this instance tells us about it) comes
after the merge and is not here.

## How to run

- Each session gets: your preflight line, the **body**, **one ask**, and the
  family's **flavoring line** from the block at the end. Nothing else.
- Claude loads the four files from disk. Codex gets them attached (pasted).
- Six sessions: gaps × {claude, codex}, repetition × {claude, codex},
  boundary × {claude, codex}.
- Each output lands beside this file as
  `reads/<ask>-<family>.md` so the merge can read them.
- The merge session reads the four docs first, the six outputs after. Its
  prompt gets written when the outputs exist.

## Body

```
ok so here is the goal first.

we have three build outs that all started from one common baseline, the current code and its architecture as written down in /mnt/data/projects/Softland-ecs-layer/docs/below-the-waist/ecs-layer-2026-09-08/BASELINE.md. we started with a hypothesis: that there is an ECS layer on which things can be built out, the minimal layer on top of the existing code, since this is all supposed to be done on an ECS layer and we don't have that right now. instead of thinking about it more we put money where mouth is and built it out three times, in parallel, each from its own plan. so the three builds are tests of that hypothesis, not products. what we have now is concrete: the code each of the three wrote, plus the documentation they wrote about it.

the ground for this is the documentation, not the code. don't read the code the three wrote, and don't go hunting in the existing code either. the write ups they left should be enough to tell what was built and why, that is what those handoffs are for, and the baseline is the documentation of what the current code base is, so that nobody has to go looking for the code that exists. let's take it that these documents did their job properly and reason on top of them. where they don't say, say so, that is a finding for me. four files:
- /mnt/data/projects/Softland-ecs-layer/docs/below-the-waist/ecs-layer-2026-09-08/BASELINE.md, what we currently have
- /mnt/data/projects/Softland-ecs-layer/docs/below-the-waist/ecs-layer-2026-09-08/ECS-LAYER-WRITEUP.md
- /mnt/data/projects/Softland-ecs-workspace-20260908/docs/ecs-workspace/SESSION-HANDOFF.md
- /mnt/data/projects/Softland-ecs-workspace-20260908/docs/below-the-waist/vantage/ROUND-ACCOUNT.md
the last two came from two sessions that built on one branch, so they describe two designs that share a tree.

as the builds came up i asked all three the same things: is this built on top of what exists in softland or built using softland, or did you add dom based code for the ui; how is this related to building softland with softland; show me what we did on the ecs layer and what we did directly in code. their answers are in the write ups. none of the three has been judged against the others, there is no verdict to carry.
```

## Ask 1 — gaps

```
so: the diff between what we currently have and what we don't have. from the baseline and the three write ups, what did each of the three have to add that the baseline didn't give them ... the pieces that need to be built out in the code before the next build, and which of them all three needed.
```

## Ask 2 — repetition

```
so: what did all three build on their own that is the same thing? three builds, three plans, three vocabularies ... where they made up the same thing in different shapes, what is that thing, and what would it be as one thing in the layer. it counts if the next build gets smaller for it, or if more of it lives in records than in code.
```

## Ask 3 — boundary

```
so: in each of the three, what moved into records and what stayed in code, and why did it stay. across all three, what is the code that wouldn't move, and what would it take for it to.
```

## What each pair should show

Written before any output exists, so the round can check it.

- **gaps.** Codex returns the nouns as the docs state them; Claude returns
  fewer nouns with a shape under them. A shorter Claude list is the
  generalization leaking in early. Gaps is the control for repetition: if
  the one thing from ask 2 does not account for the nouns from ask 1, the
  generalization is wrong.
- **repetition.** Claude sees sameness across the three vocabularies; Codex
  reports three things unless a write-up itself says they are one. Where
  Codex agrees, the sameness is in the docs, not in the reader.
- **boundary.** Codex transcribes each write-up's own records/code split;
  Claude says why the line fell where it did. If the two agree, boundary was
  already answered by the three questions in the body, and this ask was
  input, not a room.
- **silences.** Every session says where the docs did not say. The merge
  checks whether the silences coincide; where they do is where the docs are
  not enough. A silence goes back to the builder session that made it, not
  to the code.

## Flavorings

One line, appended after the ask. Claude's is the same for all three asks.

```
claude: load the four files named above, nothing else on disk, no code, never src/app/server/env.clj.
codex, gaps: attached: BASELINE.md, ECS-LAYER-WRITEUP.md, SESSION-HANDOFF.md, ROUND-ACCOUNT.md. i am asking what each build had to add that the baseline didn't give it, and what i want from it is the list of pieces to build out before the next build, marked by how many of the three needed each.
codex, repetition: attached: BASELINE.md, ECS-LAYER-WRITEUP.md, SESSION-HANDOFF.md, ROUND-ACCOUNT.md. i am asking what all three built on their own that is the same thing, and what i want from it is that thing named as one piece of the layer, with whether the next build gets smaller for it.
codex, boundary: attached: BASELINE.md, ECS-LAYER-WRITEUP.md, SESSION-HANDOFF.md, ROUND-ACCOUNT.md. i am asking what moved into records and what stayed in code in each build and why, and what i want from it is the code that wouldn't move across all three and what it would take.
```

## Merge — referees (level 1, run on Codex, one per pair)

Body + one tail + the codex line. Output lands as `reads/merge-<ask>.md`.
The referee aligns; it does not answer the ask and gives no verdict on the
builds. Its verdict paragraphs (expectation held/broke) are separable from
its alignment, which is verbatim with line refs.

```
ok so this is a referee job, not the answer.

three builds came out of one hypothesis, that there is an ECS layer on which things can be built out, the minimal layer on top of the existing code. we built it out three times as tests of that, and now we are reading the builds back from their write ups and a baseline of the current code, never the code itself. one fixed body, three asks, each ask read by one claude session and one codex session, both given the same four docs. the ground for you is those same four docs, so you can see what the readers saw:
- /mnt/data/projects/Softland-ecs-layer/docs/below-the-waist/ecs-layer-2026-09-08/BASELINE.md, what we currently have
- /mnt/data/projects/Softland-ecs-layer/docs/below-the-waist/ecs-layer-2026-09-08/ECS-LAYER-WRITEUP.md
- /mnt/data/projects/Softland-ecs-workspace-20260908/docs/ecs-workspace/SESSION-HANDOFF.md
- /mnt/data/projects/Softland-ecs-workspace-20260908/docs/below-the-waist/vantage/ROUND-ACCOUNT.md
no code, and nothing else on disk beyond these and the two reads named below.

i'm not asking you to answer the ask, that comes after, with all three pairs together. i'm asking what the two reads say against each other. where they agree, say it once, short. where they differ, don't paraphrase, put both sides in their own words next to each other. before either read existed i wrote down what i expected this pair to show; tell me whether it held or broke, and how. and each reader was told to say where the docs didn't say ... list where each went silent, and whether the silences are the same places. write all of it to the file named below, and reply with only the path.
```

Tail per pair: the ask as given (from the Ask sections above), the
prediction for that pair (from "What each pair should show", family names
kept), the two read paths, the output path.

```
codex: read the six files from the paths above, or attached if you'd rather paste. i am asking what the two reads say against each other, and what i want from it is agreement once, disagreement in both readers' own words, whether my prediction held or broke, and where each went silent — written to the file named.
```

## Top merge (levels 2 and 3; Sid in the chair, two chairs)

Two sessions with the same prompt, one Claude (Fable, max) and one Codex,
Sid in both chats and carrying between them. Not autonomous: the position
is worked out in the chat. The blank is Sid's calls on the three
expectations after reading the referee files.

```
ok so this is the top of the read, and i'm in the chat with you. the other family is in a parallel chat with the same prompt, and i carry between you.

three builds came out of one hypothesis, that there is an ECS layer on which things can be built out, the minimal layer on top of the existing code. we built it out three times as tests of that, then read the builds back from their write ups and a baseline of the current code, never the code. one fixed body, three asks, gaps, repetition, boundary, each read by one claude and one codex session, and each pair aligned by a referee: agreement once, disagreement in both readers' own words, whether what i expected of the pair held, where each said the docs didn't say. all of that is on disk and it is the ground here, in this order:
- the four docs the readers had: /mnt/data/projects/Softland-ecs-layer/docs/below-the-waist/ecs-layer-2026-09-08/BASELINE.md (what we currently have), /mnt/data/projects/Softland-ecs-layer/docs/below-the-waist/ecs-layer-2026-09-08/ECS-LAYER-WRITEUP.md, /mnt/data/projects/Softland-ecs-workspace-20260908/docs/ecs-workspace/SESSION-HANDOFF.md, /mnt/data/projects/Softland-ecs-workspace-20260908/docs/below-the-waist/vantage/ROUND-ACCOUNT.md
- the three referee files: /mnt/data/projects/Softland/docs/below-the-waist/ecs-layer-2026-09-08/reads/merge-gaps.md, merge-repetition.md, merge-boundary.md
- the six raw reads beside them, reads/<ask>-<family>.md, for when a quote needs the rest of its page
no code.

my own calls on the three expectations, after reading the referee files: ___

what i want is the diff between what we currently have and what we don't have, as one position: what should exist in the code before the next build, and what to build first. three things the pairs can't settle on their own because they cross them: does the one thing repetition found account for the nouns gaps found, or not; is boundary already answered by the questions i asked the builders, or did that pair find something the write ups don't say; and where the silences fall in the same place across the three, which go back to the builder that made them and which are the layer itself.
```

```
claude: load the four docs, then the three referee files; a raw read only when a quote needs its page; nothing else on disk, no code, never src/app/server/env.clj.
codex: read the same files from the paths above, or attached. i am asking for the diff between what we have and what the three builds needed, and what i want from it is one position: what should exist in the code before the next build, what to build first, and which silences go back to a builder.
```

After the top merge: the meta prompt, its own session, evidence = the
chats. Not written yet.
