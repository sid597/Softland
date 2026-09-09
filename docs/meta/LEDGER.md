# Ledger — how the loop has changed

One entry per change in the loop's shape. Dates are the fact here. Newest
last. Each entry: what changed, what caused it, where it landed.

```
    2026-09-05  comparison rounds ─┐
    2026-09-06  two chairs         │  one kind at a time,
    2026-09-07  seven explorations │  prompts become the object
    2026-09-08  the room, the skill, three builds
    2026-09-09  the read: asks × families, merge, meta split
    2026-09-09  the merge gets its levels: referees, top merge, bias
                                   ▼
```

## 2026-09-05 — comparison rounds, and the family bias

Paired deliveries on the path kind and 3D, every session asked to rank
every other. Found: each model family ranks its own family higher, Codex
rankers partition perfectly and replicate one judgment. Rule from it: fix
the criterion first, weight a family as one vote, keep one session of each
family open. In exploration, framing scores and detail errors go on a fix
list, never into a rank.
→ memory `feedback-cross-model-rankings-family-bias`,
`feedback-exploration-rank-framing-over-correctness`.

## 2026-09-06 — two chairs written down

Composer and definer, the cycle's two phases, the guards, three prompt
kinds, a production section. The prototype is the spec; no contracts; tests
built as you build. This is the loop's shape when it was one kind at a time.
→ `docs/below-the-waist/two-chairs.md`, memory
`feedback-implementation-rounds-no-contracts`.

## 2026-09-07 — seven explorations, and the prompt becomes the object

Path and 3D complete. One question, "what should we focus on next and how
to even think about that," through seven exploration sessions; each fed the
others' answers and asked to rank. Then the prompt itself was studied: could
the prompts for Claude have been better. Sid's rule: Claude explores best
with no logs or docs loaded, terrain in the message, code only if a claim
needs a receipt.
→ `docs/below-the-waist/visioning-2026-09-07/`, memory
`feedback-exploration-starters-direction-not-shape`.

## 2026-09-08 — the room, the skill, three builds

Nine rankings combined. The hypothesis: Codex does what the words say,
Claude becomes what is in the room, the biggest thing in the room wins. The
prompt-maker skill: raw prompt in Sid's first person plus one-line
flavorings per room; Sid decides what to append and to how many sessions.
Build prompts for Codex go job first, no pre-hunted receipts. Three plans
(two from Claude sessions, one Codex's own) built overnight by three Codex
sessions in two worktrees, each build a test of the ECS-layer hypothesis.
→ `.claude/skills/prompt-maker/`, memory
`feedback-implementation-prompts-job-first-codex-hunts`,
`docs/below-the-waist/vantage/`, `docs/below-the-waist/ecs-layer-2026-09-08/`.

## 2026-09-09 — the read step gets its shape; meta split off

The builds are read from their write-ups and a baseline of the current
code, never the code itself: trust the docs, record where they are silent.
One fixed body, three asks (gaps, repetition, boundary), each in both
families, six sessions in parallel; predictions written before outputs;
silences go back to the builder session that made them; a merge session
reads the docs first and the six reads after. Meta is its own prompt, run
after the merge, because its evidence is the chats and in one room the meta
wins. This folder opened.
→ `docs/below-the-waist/ecs-layer-2026-09-08/ASKS.md`, `docs/meta/`.

## 2026-09-09 — the merge gets its levels

Six reads landed as files. The merge is the fan-out run backwards: pairs,
then across asks, then the goal. Level one is a referee per pair, Codex,
autonomous: agreement once, disagreement in both readers' own words with
line refs, the written expectation held or broke, where each went silent;
not the answer, no verdict on the builds. Family bias in referees was
raised and blinding rejected, style leaks; the verdict is treated as
separable from the verbatim alignment, and the family check moves to the
top merge, run as two chairs with Sid between them. First referee result:
the gaps expectation broke, both readers generalize, Claude's all-three
list longer than Codex's, reported by a Codex referee against its own
family.
→ `docs/below-the-waist/ecs-layer-2026-09-08/reads/`, `ASKS.md` Merge and
Top merge sections.
