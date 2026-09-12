# Block text corpus extraction — source, method, and checks

## Source

Archive: `/mnt/data/projects/Softland-archive-20260824T105543Z-497e11e`
(source HEAD `497e11ecdce834ea69a76f45b2a85f1bc352353a`, snapshot started
`2026-08-24T10:55:54Z`).

PState file used (the ONLY one that stores per-block *current, live* text
keyed by a block uid in the `…ep:<hash>:000000` form the task described):

```
pstates/app.server.rama.object-container_object-container-module___unit-graduations-by-id.ednl
```

Manifest entry: `$$unit-graduations-by-id`, module
`app.server.rama.object-container/object-container-module`, shape `:flat`,
`entry-count 144`, `leaf-count 144`, value-class `UnitGraduationRow`.

Key field used: `:current-content-text` (inside `:archive/value`). The
`:archive/key` (== the row's `:unit-id`) is the block uid, e.g.
`du:chat:77088a4f...:episode-native-v0:ep:3c6512e2:000000`.

Secondary file (join only, not a text source): `…derived-units-by-id.ednl`,
used to fetch `:unit-kind` per uid for the `machine` field (see below).

## How block text was identified vs other material

`containers-by-id.ednl` (5322 rows) holds many `:container-kind` values
(`chat-message` 3213, `document` 1013, `tool-result` 556, `tool-call` 450,
`text-block` 42, `chat-conversation` 22, `facet-master` 22, `assembly` 4).
Of the 42 `:text-block` rows, 20 are `oc:block:chat:<hash>:NNNNNN` (real
workspace canvases/boards) and 22 are `oc:block:fm:...:active-pointer`
(facet-master revision pointers, unrelated UI material — excluded).

Each of those 20 canvas containers is a *slot* that can be repeatedly
overwritten: a human pastes/replaces its content over time. `containers-by-id`
only holds the **current** occupant of each of the 20 slots. The full
history of "a unit was graduated (pasted in) as the live content of a block"
lives in `unit-graduations-by-id` — 144 rows, one per **distinct graduation
event** (i.e., per distinct uid that was, at some point, the live text of a
block). This is why 144 > 20: several slots were pasted into more than once
over the workspace's history (confirmed directly — see Uncertainty below).

This is the file/field that matches the task's example uid shape exactly and
reproduced both named check values exactly (see Extraction), so it is used
as the block-text corpus.

`derived-units-by-id.ednl` was cross-referenced only to pull `:unit-kind`
per uid, to attempt a `machine` classification (see below) — it is not a
text source for the corpus (its own `:derived-content-text` field holds the
*original, pre-edit* ingested text, which can differ from the block's
current/served text after edits; `current-content-text` from
`unit-graduations-by-id` was used instead as the "served" value).

## Extraction

- 144/144 rows in `unit-graduations-by-id.ednl` parsed with Clojure's
  `clojure.edn/read-string` (namespaced-map `#:archive{...}` forms parse
  natively; no custom EDN readers were needed — no tagged literals other
  than plain EDN appear in this file).
- `chars` = `(count current-content-text)` in Clojure/Java, i.e. UTF-16 code
  units, per the requested unit.
- 144 distinct uids, spanning **18 distinct chat workspaces** (one dominant
  workspace `77088a4f...` contributes 119 of the 144 blocks; the other 17
  contribute 1–6 each).
- Total chars: **334,537** (sum of the `chars` field across all 144 rows).
- 15 of the 144 blocks currently have `chars: 0` (graduated once, later
  edited down to empty text) — included in `corpus.json`, not filtered out,
  since "every block that has text" was read as "every block row with a
  text field," not "every block with non-empty text." Flagging this so the
  benchmark can decide whether to drop them.
- Size range: min 0, median 201, max 96,727 chars; 5 blocks exceed 10,000
  chars.

### Check blocks (both confirmed exact)

| uid contains | target (given) | extracted | match |
|---|---|---|---|
| `3c6512e2` | 24,891 chars | **24,891** chars (`current-content-text` on `du:chat:77088a4f...:episode-native-v0:ep:3c6512e2:000000`) | exact |
| `3405ac6d` | 66 chars | **66** chars, text `"#Task 16 \nI want to select block and delete it like if i do shift+"` | exact |

Both were found via `unit-graduations-by-id.ednl`, key match on the full
`:archive/key` string (not a substring grep, which over-matches — a plain
substring grep for `3c6512e2` hits 12–35 unrelated rows in other PStates
that merely *reference* the uid, e.g. inside `:visible-addresses` lists).

## machine field

No field in this archive tags graduated blocks as "machine"/"assistant"
origin. `derived-units-by-id.ednl`'s `:unit-kind` taxonomy (tallied over all
43,211 rows) has no `:machine-message`/`:assistant-message` value at all;
the `episode-native-v0` distiller (399 rows total, the one that produces the
`ep:<hash>:000000`-shaped uids) only ever emits `:human-message` (163),
`:human-sub` (207), or `:material-part` (29) — the distiller apparently
handles only the human side of a chat turn this way. Cross-referencing all
144 corpus uids against this file: **141 `:human-message`, 2 `:human-sub`,
1 `:material-part` — zero of any other kind.** Every row got a real,
verified value (no lookup misses).

Given that, `machine` is set to `false` for all 144 rows (they are, per the
only available field, catalogued as human-side content — even though a
block's *pasted content* can itself look machine-authored, e.g. the
`3c6512e2` block's text visibly reads as a Claude response that Sid pasted
into his own turn). This directly conflicts with the target's "36 machine
blocks" — see Uncertainty.

## Uncertainty (why totals differ from the target ~174 / 36-machine / 586,927)

- **Block count: 144 vs ~174.** Could not determine the extra ~30. Checked
  and ruled out: adding the 20 canvas containers or the 22 facet-master
  pointer containers from `containers-by-id` doesn't cleanly land on 174,
  and the facet-master ones are a different material family (not chat
  blocks). Most likely explanation, not verified: the founding capture's
  "174" was taken at a different point in the workspace's live history than
  this archive's 2026-08-24 snapshot (see next point), so the exact set of
  graduation events differs.
- **`3c6512e2` is confirmed superseded as of this snapshot.** Its container
  slot (`oc:block:chat:77088a4f...:000000`) was last touched by this uid on
  2026-07-26 (`:last-revised-at-ms` → `2026-07-26T10:06:00Z`); by
  2026-08-12 the *same* container slot's live content in `containers-by-id`
  had already been replaced by a different uid (`ep:076f89e9:000000`). So
  the "largest block" the target names was accurate as of some earlier
  point but is not the largest in this archive's full history (max found
  here is 96,727 chars, a later/different uid) — meaning the founding
  capture's numbers likely predate several later paste-replace events this
  archive's snapshot has captured. This is a hypothesis from timestamps
  actually read in the data, not a guess.
- **Total chars: 334,537 vs 586,927.** Not reconciled. Given the block-count
  gap and the confirmed-superseded example above, a plausible contributor is
  that the founding capture summed served/projected text (which may include
  more than raw `current-content-text`, e.g. rendered markdown or included
  child material) rather than this raw field — unverified, flagged as a
  hypothesis only.
- **`machine`: 0 vs target's 36.** The archive's `:unit-kind` taxonomy has
  no machine-origin tag at the block level at all (see above) — this looks
  like a genuine schema gap rather than an extraction miss (every uid got a
  real lookup hit), but it's reported, not asserted as the full explanation.
- Text used is `current-content-text` — the text as most recently edited/
  served for that specific graduation uid — not the original pristine
  ingested text (`derived-units-by-id`'s `:derived-content-text` differs,
  e.g. it's 10,660 chars for `3c6512e2` vs 24,891 current/edited — the block
  grew significantly through in-place edits after its initial paste).

## Machine-block follow-up (read-only, no changes to corpus.json)

Checked all 10 of the 44 PState files whose name matches the given keywords
(`episode`, `machine`, `cut`, `cascade`, `derived-units`, `material-truth`,
`facet`, `binding-material`, `activation`, `transcript` — no file matches
`episode`/`machine`/`cut`/`cascade`/`material-truth`/`binding-material`/
`activation` at all; those application-layer concepts apparently own no
PStates of their own in this archive's 4 kept modules):

- `derived-units-by-id.ednl` — already the join source for the 144; re-tallied
  by distiller: `episode-native-v0` (399 rows, the one producing `ep:<hash>`
  uids) emits ONLY `:human-message`/`:human-sub`/`:material-part` — zero
  machine-tagged kind anywhere in the whole 43,211-row file.
- `source-derived-units-by-source.ednl` — pure index (source→derived-unit
  pointers: `:source-id`, `:target-id`, `:order-key`, `:event-id`). No text.
- `transcript-audit-by-request.ednl` (3756 rows) — audit log entries, fixed
  `:message "Transcript source record imported"` string, no content text.
- `transcript-conversation-projection.ednl` — **does** carry a `:role` field
  (`"assistant"` 2920, `"user"` 1719, `"sid"` 231, `"softland:matter-room"`
  29, `"llm:material-autotag"` 5, system-wide) plus a `:container-id`
  pointing at the raw ingested message in `containers-by-id`
  (`:container-kind :chat-message`, which does hold full
  `:current-content-text`). This is real machine-authored text — but it's
  the **raw imported transcript pool**, not a "block": just the one
  dominant workspace (chat `77088a4f...`, the source of 119 of our 144
  blocks) alone has 717 assistant-role messages — 20x too many to be "the
  36." No field anywhere marks a curated subset of ~36 of these as
  graduated/served/blocked; `unit-graduations-by-id` (the only PState with
  that concept) never draws from this role or from any assistant-shaped
  content, confirmed for all 144 rows.
- `transcript-last-message-by-conversation.ednl` (22 rows) — one pointer
  row per conversation (`:message-container-id` only), no text, no role.
- `transcript-source-lines-by-file.ednl` / `…-transcript-ops-module___
  transcript-file-source-lines-by-file.ednl` — per-line ingestion-status
  bookkeeping (hashes, offsets, parse-error flags), no message text.
- `transcript-tool-calls-by-name.ednl` — tool-call index keyed by tool name,
  points at `oc:tool-call:...` containers, no answer text.
- `…-transcript-ops-module___transcript-file-offsets.ednl` — resume/offset
  bookkeeping per source file, no text.
- `…-transcript-ops-module___transcript-runs.ednl` (5 rows) — harvest-run
  metadata (paths, counts, status), no text.

Also checked, off the keyword list but adjacent: `containers-by-id.ednl`'s
non-standard `:created-by` values (`sid-gate`, `fable-p6-gate`,
`softland:p8`, `sid` — 10 rows total) — all are facet-master/document
pipeline artifacts (release wishes, activation pointers), unrelated to chat
text.

**Conclusion: not found as a distinct "graduated machine block" entity.**
Machine/assistant-authored text exists abundantly in this archive (thousands
of chat-message containers with `:role "assistant"`), but nothing in any of
the 44 PStates marks a ~36-row subset of it as having become a workspace
block the way `unit-graduations-by-id` does for the 144 human ones.
`corpus.json` is unchanged (still 144 entries, all `machine: false`).

## Files

- `/tmp/claude-1000/-mnt-data-projects-Softland/9713c6f8-c848-483a-9664-1859f96d3037/scratchpad/corpus.json`
  — 144 entries, `{uid, text, chars, machine}`, valid JSON (parsed and
  spot-checked with Python).
- Intermediate scratch files (not deliverables): `unit-ids.txt`,
  `grads-extracted.edn`, `kind-by-unitid.edn`, `matched-derived-units.ednl`,
  `grad-3c6512e2.ednl`, `grad-3405ac6d.ednl`, `sample-3c6512e2.ednl`, small
  `.clj` scratch scripts — all in the same scratchpad directory.
