---
name: reference-safeguard-stops
description: "When a response is stopped by a safeguard: what the refusal categories mean (official definitions), what set off the reasoning_extraction stops of 10 and 11 November 2026, how to list stops from the transcripts, and what to do after one"
metadata:
  type: reference
---

A stopped response comes back with `stop_reason: "refusal"`, and `stop_details.category` names
the reason. The official definitions are in Anthropic's guide "Prompting Claude Fable 5.2",
section "Safeguard refusals"
(https://platform.claude.com/docs/en/build-with-claude/prompt-engineering/prompting-claude-fable-5-2):
`cyber`, `bio`, `frontier_llm`, `reasoning_extraction` ("the request asks the model to reproduce
its internal reasoning in the response text") and `general_harms`. The guide's fix for the
fourth: remove instructions that ask the model to write its reasoning in the response. The
earlier guide, "Prompting Claude Fable 5"
(https://platform.claude.com/docs/en/build-with-claude/prompt-engineering/prompting-claude-fable-5),
adds that you can still ask for a short explanation of the answer or a summary of the actions
taken.

**What was found on 11 November 2026.** Checked in the transcripts under
`~/.claude/projects/-mnt-data-projects-Softland/`: 175 stopped responses in this project since
August, 148 of them `reasoning_extraction` and 27 `cyber`. All 64 stops of 10 and 11 November
were `reasoning_extraction`, all on Fable 5.2, and each landed while the response held only
thinking, before any visible text. Opus 5.5 and Sonnet 5.5 ran 101 responses in those two days
with no stop; Opus had been stopped in the same category in September and October.

Two things went with the stops, and they were not separated from each other:

- A prompt that a session had worded asked for the thinking as the thing delivered ("I want
  your own thinking", "I want all of the thinking", "what never leaves your own context"). It
  was stopped on the first response of each of four sessions that got it (the text was read in
  one of them; for the other three, the stopped responses described the same prompt). Sid's own
  wording of the same ask, on the same notes, ran clean.
- The record's start page was then given at session start, and it carried pasted error text and
  stopped prompts into every new session. After it was taken out of session start, an opening
  prompt that had been stopped in the session before ran clean.

That the stop comes from scoring the thinking as it streams is a reading, not a checked fact.

**How to list the stops.**

```
rg --no-filename '"stop_reason":"refusal"' --glob '*.jsonl' ~/.claude/projects/-mnt-data-projects-Softland \
  | jq -r '[.timestamp, .message.model, .message.id, (.message.stop_details.category // "none")] | @tsv' | sort
```

A row with model `<synthetic>` is the error text that Claude Code shows. Count a stopped
response once, by its `message.id`.

**What to do after a stop.** The automatic retry was stopped again in 24 of the 64 cases, so
rewind (Esc twice) and reword the ask instead of letting it retry. Ask for the view and its
grounds. Two fresh sessions whose opening message was a paste of another session's diagnosis of
the stops were stopped on their first response (while the start page was still on), so do not
open a session with that material. `switchModelsOnFlag` covers only the `cyber` and `bio`
categories, by the Claude Code docs (https://code.claude.com/docs/en/model-config), so it does
not help with this one. `/feedback` is the route to a fix at the source.
