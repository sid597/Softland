# Block types — minimal dataset (message 2 of 2)

Stand-in for a real dataset: every block type that exists, the structure
between them, placeholder text only. Generate your own filler at whatever
density a screen needs — assume ~30 turns like the example below, with the
length variance noted per type.

## Id shape

`du:chat:<conversation-key>:block-v0:<event>:<part>:<block>`
event = position of the message in the conversation · block = position
within that message. Elide the key when rendering (`…:000012:00:000003`).
Ids are stable and real — shown, but subordinate to the text.

## Type catalog

| type | actor | text length / texture |
|---|---|---|
| user-message | user | whole message as typed: 1 line to ~30 lines, run-ons, ellipses, occasional all-caps shouting |
| user-sub-chunk | user | one paragraph of its parent message; parent AND children both exist; 2–6 per message |
| agent-thinking | agent | internal reasoning; the longest blocks: ~200–1500 words; usually first in a response |
| agent-prose-paragraph | agent | one paragraph of the reply; 1–8 per response |
| agent-list-item | agent | one bullet; comes in runs of 3–10 |
| agent-tool-call | agent | one code-ish invocation line |
| tool-result | tool | what came back: 1 line to hundreds, log/code-ish |

## One turn, fully shaped (order and nesting are real; text is placeholder)

    [event 000012 — the user's message]
    user-message       …:000012:00:000000   [placeholder — 3 paragraphs, ~10 lines]
      ├ user-sub-chunk …:000012:00:000001   [placeholder — paragraph 1]
      ├ user-sub-chunk …:000012:00:000002   [placeholder — paragraph 2]
      └ user-sub-chunk …:000012:00:000003   [placeholder — paragraph 3]

    [events 000013–000016 — the agent's response]
    agent-thinking         …:000013:00:000000   [placeholder — ~800 words]
    agent-prose-paragraph  …:000014:00:000000   [placeholder — 3 sentences]
    agent-prose-paragraph  …:000014:00:000001   [placeholder — 1 sentence]
    agent-list-item        …:000014:00:000002   [placeholder]
    agent-list-item        …:000014:00:000003   [placeholder]
    agent-list-item        …:000014:00:000004   [placeholder]
    agent-tool-call        …:000015:00:000000   [placeholder — one line]
    tool-result            …:000016:00:000000   [placeholder — ~40 lines]

A turn = the user message + the whole agent response to it. A conversation
= ~30 such turns in sequence. Vary the mix across turns: some with no
tools, some with 5 tool calls in a row, some replies a single line, one
thinking block enormous.

## Also true

- Tool/system noise events exist in the data (retained, not main flow) —
  de-emphasize, never delete.
- Whatever page is shown is page 1 of a longer conversation
  (`page-complete? false`) — the view needs an answer for "there is more."
