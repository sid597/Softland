# Token Economy — measured receipts (W4 + T2)

Measured by an Opus session from transcripts (usage records deduped by
requestId, not estimates, except where marked); adjudicated and landed by the
Fable session of 2026-08-07. The BINDING operational core lives in CLAUDE.md
"Token Economy" — this file is the receipts behind it. The ranking of levers is
the lesson; the percentages are session-shaped, not constants.

**Fable adjudication of trust:**
- Endorsed as fact: the carry mechanism (size × requests-remaining), ~1:1
  thinking retention (the window-check method is sound), reads as the top
  controllable line, the post-Write tail numbers, the 25k-token Read cap, the
  ~42k boot floor.
- Endorsed as actionable hypothesis: the 702k prefix-rewrite attribution —
  cost verified, cause inferred from temporal correlation + known mechanism
  (the report says so itself). No probe needed; the defense is free.
- Rejected / down-weighted: the ~5k-token universal shard quota (shard along
  semantic seams — contract, package — never to a byte target; the budget
  governs what you LOAD, not how law is cut); any window-proportional
  attribution percentage (raw sizes only — the report's self-flagged error #1).

---

## The report (verbatim)

Two sessions, measured from transcripts. All numbers are from usage records
deduped by requestId, not estimates, except where marked.

### 1. Headline

```
                         W4 cut f35b64b0             T2 cut 578fc7fd
                         (complete)                  (stopped)
model / effort           claude-fable-5 / max        claude-fable-5
parent requests          68                          20
final context            455,733                     336,939
parent billed input      22,925,056                  4,330,382
parent output            163,165                     106,144
subagents                4 Opus, 91 req              5 Opus, 119 req
subagent billed input    6,827,654                   12,607,708
grand total billed input 29,752,710                  16,938,090
```

Cost-weighted (cache read 0.1×, 1h write 2×, 5m write 1.25×), W4 ≈ 5.66M
base-rate-equivalent input tokens.

The number you watch is not the number you pay. 455k was context occupancy on
the last request. The bill was 22.9M on the parent alone, because all 68
requests re-send the whole prefix.

### 2. The mechanism

Context cost is quadratic. Every request re-sends the entire prefix. A token's
real price is size × requests remaining — call it carry. A token landing at
request 5 of 68 is paid for 63 more times; the same token at request 60 is
paid for 8.

Three sub-facts, all verified rather than assumed:

- Model output re-enters the prefix ~1:1, including thinking. Request #16
  emitted 20,931 output tokens; request #17's cache_creation was 21,034.
  Across 68 requests, zero windows had prev_output > cache_creation —
  thinking is retained, not stripped.
- Cache invalidation is total, not partial. Anything that changes content
  before a point invalidates everything after. The system prompt is at
  position zero.
- The boot floor is ~42k per session (system prompt + tool schemas +
  CLAUDE.md + skill listing), paid before you type. This is the tax on
  splitting sessions.

### 3. W4 — carry-cost ranking

Tokens × requests remaining. Total carry 20.96M.

```
rank  item                                              size     carry   %
1     assistant own output (thinking + text + tools)    156,154  6.57M   31.3%
2     subagent verbatim returns (3 notif, 168,012 ch)    78,078  4.16M   19.8%
3     ENGINE.md                                          25,800  1.65M    7.9%
4     CHROME-ATOM-CONTRACT.md                            22,190  1.33M    6.4%
5     W0-C.md                                            18,035  1.14M    5.4%
6     W1.md                                              17,802  1.10M    5.3%
7     decisions.md                                       17,475  1.07M    5.1%
8     Sid's typed prompts                                22,865  0.93M    4.4%
9     skill_listing (injected 3×)                        11,988  0.78M    3.7%
—     all 52 Edits + 2 Writes combined                    5,509  0.16M    0.7%
```

Reads #3–#7 combined = 6.29M carry (30%).

The writing was not the problem. 52 edits and 2 writes cost 0.7% of carry —
cheap only because they landed at requests 46–67 with almost no session left
to re-read them.

### 4. W4 — the invisible 702,621

Two requests rewrote the entire cached prefix:

```
time      cache_creation  cache_read    preceding event
19:37:47  322,018         25,058        permission-mode flipped default → auto
19:51:35  380,603         0 (ice cold)  /remote-control active, bridge attached
```

At the 1h-cache write rate (2×) that is 1.4M effective input tokens — ~32% of
the main session's entire cost — for zero new content.

TTL expiry is ruled out: gaps were 25 min and 14 min against a 1-hour cache.
The numbers are verified; the causes are inferred from tight temporal
correlation plus known mechanism (both live in the system-prompt region).

### 5. W4 — phase split

```
phase                           requests  billed input  output
boot + gather (19:12–19:29)     17        2,853,854     84,499
cut / decide (19:34–19:37)      13        4,438,919     16,752
draft + repair (19:49–19:53)    16        6,046,959     25,591
post-Write tail (19:53–20:06)   22        9,585,188     36,323
```

The last 32% of requests consumed 42% of the bill and produced 22% of the
output, at 400k+ depth doing the most mechanical work in the session (52
fix-by-fix edits).

The 39,326-char Write landed at request 16 of 68, at ~307k context — then 52
edits followed across the remaining 52 requests.

### 6. T2 — context composition at 335,954

Raw sizes, reconciled to within 4%.

```
bucket                                                     tokens    %
C. full-document reads — 14 docs, 364,955 chars            132,711   39.5%
B. model's own output (incl. req#15 = 37,840, the 44KB
   contract Write)                                         103,388   30.8%
A. boot floor (system + tools + CLAUDE.md + skills)         41,753   12.4%
E. typed prompts (62,384 ch) + harness attachments          30,472    9.1%
D. gatherer returns + all other tool results                13,963    4.2%
```

The read list:

```
next-prompt.md                  76,370 ch   (read twice — truncated)
W4-FRAME-RUNTIME-CONTRACT.md    54,919 ch
CHROME-ATOM-CONTRACT.md         54,886 ch
W1.md                           46,930 ch
decisions.md                    46,298 ch
CONTRACT.md                     29,526 ch
W0-C.md                         13,962 ch
+ 7 more                        42,064 ch
                               ─────────
                               364,955 ch
```

Delegation cost 4%. Documents-read-whole and thinking were 70%.

### 7. The read cap, and why it's the wrong constraint

Actual notice from the session:

```
[Truncated: PARTIAL view — docs/next-prompt.md: showing lines 1-707 of 1016
 total (30502 tokens, cap 25000). Call Read with offset=708 limit=707 for the
 next page]
```

Cap = 25,000 tokens per Read call (and 2,000 lines, whichever binds first).
At ~2.5 bytes/token for these docs, that's ~60KB.

```
doc                            bytes   ~tokens  vs cap
next-prompt.md                 73,489  30,502   over — truncated, paged, read 2×
CHROME-ATOM-CONTRACT.md        52,346  ~21,000  16% headroom
W4-FRAME-RUNTIME-CONTRACT.md   52,369  ~21,000  16% headroom
W1.md                          43,982  ~18,000  ok
decisions.md                   44,149  ~17,000  ok
```

But a 150k session's entire read budget is ~40k tokens (~110KB). One
maxed-out Read eats 62% of it. Designing docs to fit the cap is meaningless
when 2.5 such docs blow the session. The budget, not the cap, is the design
target — which puts the right shard size at ~5k tokens ≈ 12KB.

### 8. Document structure — where the bytes are

```
doc                        biggest sections
next-prompt.md 73,489 B    Active package blocks 40,800 (55.5%, 27 LIVE
                           blocks) · P8 GATED PASS 20,061 (27.3%) · live
                           board 11,473 (15.6%)
W1.md 43,982 B             Contract G 10,261 · T 5,644 · D2=A 5,149 ·
                           C 4,575 · O 4,211 · M 4,005
CHROME-ATOM-CONTRACT       Scope 20,420 (39%) · Entry points 9,912 ·
52,346 B                   Scenarios 7,873 · Laws—pointers 2,844 (5.4%)
decisions.md 44,149 B      render seam 11,673 · How we work 8,088 ·
                           Settled arch 8,024
```

Two things fall out:

The docs are already sharded by the thing that decides relevance. W1 is nine
contracts in one file; a text atom needs Contract T (5,644 B), not 43,982 B.
The natural section boundaries are already the right shard boundaries.

CHROME-ATOM-CONTRACT.md already contains the solution — a section literally
titled "Laws — pointers, not restatements", 2,844 B, indexing 44KB of W1 with
the specific obligation and operationalizing scenario per contract. The
principle was applied to the writing side and never to the reading side.

An index of pointers is safe where an index of content is not. The
read-primary law exists because summaries drop mandates — the path-atom round
lost 3 of 8 findings that way. A pointer doesn't restate the mandate, it
locates it, and you still read it from source. That's the precise distinction
that lets section-reading comply with the law instead of violating it. It
also means naive folder-sharding without a spine makes the mandate-miss
problem worse, not better: an agent reading contract-T-text.md has no signal
that Contract G binds it.

### 9. T2 subagents

```
agent                             requests  billed input  final return
Index frame runtime + tests       38        4,016,543     19,774 ch
Index selection/pick/clip roads   30        3,725,606     20,446 ch
Index text layout seam            25        2,723,705     15,373 ch
Index text input status quo       20        1,878,813     16,732 ch
Lens 1+4 sweep (killed at 27s)     6          263,041        109 ch
```

Two findings:

The gatherers were disciplined. All four self-bounded and wrote full dumps to
scratchpad (226,837 B across 4 files) — returning ~81k chars ≈ 28k tokens
total, versus W4's 168k chars ≈ 78k tokens. That's the single practice that
improved between the two sessions.

They duplicated 180,943 chars (~62k tokens) of reading. text_layout.cljc read
twice at 75,356 ch each, ground.cljs by three agents, plus frame_graph.cljc,
frame_runtime.cljs, renderer.cljs, compositor_gpu.cljs twice each. Sharding
by topic over a codebase where topics share a substrate produces overlap;
sharding by file region wouldn't.

### 10. Errors in this analysis

Three, all the analyst's, stated because they affect how much to trust the
rest:

1. skill_listing estimated at 27,992 tokens (16.5%); raw is 14,941 chars ≈
   4k. Proportional attribution over-assigns to small items when a large
   cache_creation shares a window with them. All raw-size figures above are
   safe; that method is not.
2. Predicted ~160k tokens of gatherer verbatim landing on T2, called bounding
   it worth ~9M. Actual: ~28k. The gatherers had already self-bounded.
3. Called next-prompt.md "85% archive." It is 27 live package blocks with
   status, rulings and triggers. The correct sharding axis is per-package,
   not archive-vs-live.

### 11. What generalizes

Ranked by measured effect, not plausibility:

1. Reading is the biggest controllable line — 40% of T2's context, 30% of
   W4's carry. Count bytes before booting; over budget means cut documents,
   because nothing else moves enough.
2. Depth × requests, not depth — W4's tail was 32% of requests for 42% of
   the bill doing mechanical work. A fat session is safe if it's short and
   ends in a file.
3. Never mutate the system prompt mid-session — 702,621 tokens, ~32% of W4's
   cost, zero content.
4. Thinking is a dial — 31% of W4's carry, re-read by every later request.
5. Delegate typing, never authoring — the test is whether the input
   compresses. Gathering compresses (question in, digest out). Typing
   compresses (edit list in, confirmation out). Authoring compresses on
   neither: its input is the session's whole accumulated judgment.
6. Bound gatherer returns — already working in T2; worth ~50k tokens of
   context versus W4.
7. The 42k boot floor is the splitting tax — three sessions pay 126k instead
   of 42k. Still the right trade at 2–3 parts; stops paying at five.

---

## Codex receipt — FRAME-VIEW/REGION-BINDING, 2026-08-09

Source:
`~/.codex/sessions/2026/08/09/rollout-2026-08-09T23-20-25-019fe7a5-b9b9-7b33-a0bc-141c4561df31.jsonl`.
Measured from literal rollout records; encrypted reasoning was not used.

```text
model / effort             gpt-5.6-sol / xhigh
original turn              58m 16s
input before first abort   33,705,923
cached input               33,115,136
output / reasoning output  116,699 / 55,049
peak request input         242,055 / 258,400 (93.7%)
token-count events         243
Codex exec orchestrations  228 total session
nested actions             179 shell · 64 patch · 2 plan · 1 stdin
tool-output text            ~1.51M chars · 26 returns over 20k chars
compactions                18:27:16; 18:50:59 UTC
```

The original turn contained only the first compaction; the second followed
Sid's interruption and a restarted turn. The 228 records were orchestration
calls, not 228 literal shell calls. `297 reasoning` records were also not 297
requests. These distinctions are structural because every orchestration
roundtrip can carry the accumulated prefix.

The tool-output row counts text from both custom and function-call outputs.
The NOW close entry was patched at 18:43:58. Before the first abort, another
16 orchestrations ran, containing 20 shell invocations and 2 patches. That
tail added 3,874,624 input tokens (11.5% of the original turn). The prior
"zero patches after 18:45:40" observation is true but does not establish a
terminal NOW boundary: both late patches already followed the NOW write.

The close-receipt stretch from about 18:25 to the NOW write added 11,373,928
input tokens and contained 84 orchestrations, 83 shell invocations, and 11
patches. This does not prove that receipts should be skipped. It proves that
pre-registered receipt mechanics, new harness work, source repair, and close
recording must not share one deep conversational loop.

### Codex operating consequence

- The primary owns binding law, source authorship, rulings, and synthesis.
  Fresh lower-cost collector/explorer agents own bounded non-binding gathering;
  receipt runners execute already-specified receipts. Returns have no verdict
  authority and parent + children total cost is the measure.
- Implementation is batch-shaped: one collected diagnostic set, one coherent
  namespace/compile repair, one focused check. Relevance does not justify one
  model roundtrip per command or patch.
- Candidate-source freeze may self-handoff into one fresh close-receipt
  context. This is the same atom/custody, not a phase ladder or Sid touch.
- NOW is the terminal planned close mutation. Later source/test/tooling edits are
  forbidden; an invalidating find marks REPAIR/RECEIPT PENDING and resumes in
  a fresh context.
- A pre-registered close receipt is not an investigation. The investigation
  fence begins only when an unexplained result is being causally attributed.

Reproduce the stable metrics without printing transcript content:

```sh
python3 scripts/report-codex-rollout.py \
  ~/.codex/sessions/2026/08/09/rollout-2026-08-09T23-20-25-019fe7a5-b9b9-7b33-a0bc-141c4561df31.jsonl \
  --now-path docs/render-engine/FRAME-VIEW-REGION-BINDING-NOW.md
```

For a delegated session, report the whole family directly:

```sh
python3 scripts/report-codex-rollout.py --family \
  019fef0a-ce3f-7b42-af30-4341ab8c512e
```

`--family` discovers descendants from `session_meta` parent links and reports
parent, children, role totals, child share, and agent-management calls. Manual
multi-path aggregation remains available for unrelated comparisons. Do not
import Claude's 42k boot floor or a single-session percentage as a Codex
constant; establish task-class baselines from at least five families.

---

## Codex follow-up receipt — LOWER-RESOLUTION, 2026-08-11

Source parent: `019fef0a-ce3f-7b42-af30-4341ab8c512e`; eight descendants were
discovered from literal `session_meta` parent links. Encrypted reasoning was
not used.

```text
family rollouts              9 (1 parent + 8 children)
family input / total         33,096,294 / 33,248,881
parent / child input         26,235,199 / 6,861,095 (20.731% child)
nested actions               267 shell · 22 patch · 4 plan · 13 stdin
agent management             8 spawn · 18 follow-up · 24 agent-wait · 2 list · 1 send
tool-output text             1,998,116 chars · 36 returns over 20k chars
compactions                  2
peak request input           219,865 / 258,400 (85.09%)
```

Patch batching improved sharply against the 2026-08-09 session (22 vs 64),
and median/peak parent depth fell. Whole-family input fell only about 4.6%
against that earlier full rollout's 34,689,991 because eight persistent
children and their management/output consumed most of the parent saving. Before the first patch,
the parent spent 5,846,288 input tokens across 33 execution and 37 agent
management calls. After source freeze it spent about 6.8M more input reaching
close, while the fresh final receipt runner itself used only 155,546.

This is the corpse for the one-wave/one-return and compacted-primary handoff
rules. They expire after five representative implementation families show no
persistent-agent loop and compacted families close in a fresh primary. The
receipt does not justify skipping the contract-required live preflight,
abolishing subagents, or setting a universal numerical token ceiling.
