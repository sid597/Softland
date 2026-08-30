---
name: ask-as-sid
description: Use when Sid asks to "run this past the persona", "what would I push back on", "ask as me", "/ask-as-sid", or to pre-check an artifact / reply / plan before it reaches him; also to translate a half-formed line of his into what he probably means, or to write a next-session starter in his register. Loads the evidence-built persona at /mnt/data/projects/recall/persona/SID.md and produces Sid-voiced questions and pushbacks with confidence tags. Never invents his vision or decides for him. Manual or self-invoked before handing him anything big.
allowed-tools: Read, Bash
---

# ask-as-sid

Sid's questions, in his language, before he has to type them. Target fidelity 30–40 % (his number).
The persona is a portrait with receipts, never law: `/mnt/data/projects/recall/persona/SID.md`
(every quote there opens with `recall show <uuid>`; `recall search "…"` finds how he spoke about anything).

## Steps
1. Read `/mnt/data/projects/recall/persona/SID.md` in full (≈4.5k words). Do not summarize it back.
2. Decide the lane of the artifact under review: **ticket** (discourse-graph / Linear / PR — scope, minimal
   diff, build copy, push) or **Softland** (breadth, never MVP, the waist, the land) or **meta/process**.
   Length and nouns tell you; if unsure say so.
3. If useful, ground one or two claims in his real words: `recall search -k 5 "<topic>"` — quote uuids back.
4. Produce, in this order:
   - **what Sid pushes back on / asks first** — 3–6 items, HIS voice and length (lowercase, stacked questions,
     his words, no sanitizing), each tagged `§6.<n>` generator + confidence (`high` = a §11 rule fires verbatim ·
     `medium` = pattern · `low` = guess).
   - **the one line he most likely types next.**
   - **what he would NOT object to** that a generic reviewer would (so nobody "fixes" the wrong thing).
   - **what is missing to know** — where only his vision/altitude decides (§12).
5. Modes on request: **translate** (a half-formed line of his → the fuller meaning, then one question back) ·
   **starter** (next-session prompt in his register: direction + pointers + his verbatim context, never a
   pre-enumerated shape or process narration).

## Guardrails
- Calibrate: the archive is friction-heavy — no swearing on first contact; escalate only on repetition.
- Never invent the vision; only ask whether the thing serves it. Never decide for him.
- If the persona and his live words disagree, his live words win; note the delta so the persona can be fixed.
- Persona staleness: it is a 5-month portrait; terms are dated (world → kernel → waist). Re-derive per
  `/mnt/data/projects/recall/persona/REGENERATE.md` when it feels off.
