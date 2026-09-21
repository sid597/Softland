# First-record research, retired 21 September 2026

Earlier location: `src/proposal/first-record-2026-09-20/`. Moved here at Sid's word on 21 September
2026: "I will not tell any session to read them." What is passed on from this work is one
document, [`CORNERS.md`](../../../src/proposal/first-record-2026-09-20/CORNERS.md). Everything in
this folder is evidence under it. Open a file here to check a quote, not as a session's reading.

The question the work served, in Sid's words: "i am deciding what my store must fix before its
first record. the store only appends and never rewrites, so whatever a fact does not carry when it
is made is gone for every earlier fact. what i want first: the conventions that would paint me in a
corner."

## What is here

| path | what it is |
|---|---|
| `research/*.md` | Eight research files from seven sessions in three rounds (20 September 2026), about 310k tokens: `clocks-ids-determinism`, `defaults-skeptics-bigtech`, `facts-datalog`, `frontiers-views`, `log-as-truth`, `meaning-objects-substrates`, `rama-marz`, `sync-versioning-defaults`. Line anchors in `CORNERS.md`, such as (rama L772), are line numbers in these files. |
| `research/synthesis.md`, `the-camps.md`, `the-fact-and-its-questions.md`, `orchestrator-notes.md` | The research orchestrator's own merge. Neither applying session opened these. |
| `research/loop/` | The orchestrator's loop state and round briefs. Outside the reading fence Sid set for this work. |
| `HANDOFF.md` | The first applying session's handoff (21 September): the picture as it held it, the ledger from file one, the small way it proposed. Superseded by `CORNERS.md`, which carries the ledger in its section 14. |
| `research-trimmed/` | The pilot on file one: Codex marks line ranges, `assemble.py` copies by line number. Includes the write-up of file one, `clocks-ids-determinism.applied.md`; what still stands from it is in `CORNERS.md` section 8. |
| `corners/yardstick.prompt.md`, `corners/prompts/` | The contract given to Codex for the other seven files: the ledger embedded as the yardstick, nine tags, line ranges only. |
| `corners/plans/*.rows.tsv` | Codex's answer: 2,974 tagged line ranges (start, end, tag, ledger codes, note). Every plan tiles its file with no gaps and no overlaps. |
| `corners/bundle.py` | Copies the tagged ranges by line number into one bundle per corner, so no model retypes anything. |
| `corners/bundles/<corner>.md` | The pressed zones (round two and three, short versions, above the table, the files' own lists). The second applying session read these whole. |
| `corners/bundles/<corner>.body.md` | The same for the team-by-team body of the files. Not read, apart from nine ranges. |
| `corners/bundles/INDEX.md`, `REPORT.md` | Every copied range with Codex's note; counts, coverage and agreement per ledger line. |

## The fetched sources are not in the repository

The 363 MB of pages and papers the research sessions fetched (3,116 files) were never committed.
On 21 September 2026 they were moved to a local depot:
`/mnt/data/projects/research/softland/first-record-2026-09-20/sources/`. It is on Sid's disk only,
not in git, and not known to be backed up. The URLs are listed inside the research files. There is
no symlink on purpose: a backup tool that follows links would pull the capture back in. A
`.gitignore` rule now keeps any `sources/` folder under `src/proposal/` out of the index.

## Paths inside these records

Records keep their original wording. The Codex prompts and the first handoff name
`src/proposal/first-record-2026-09-20/research/...`; read that as this folder's `research/`. To
rebuild the bundles from the plans, from the repository root:

```
cd history/proposals/first-record-2026-09-20/corners
python3 bundle.py ../research plans bundles \
  --read defaults-skeptics-bigtech:1-25 --read meaning-objects-substrates:1-69 \
  --read facts-datalog:1045-1090 --read rama-marz:736-778
```

The `--read` ranges are the passages the session read whole itself, so they are listed in
`INDEX.md` and not copied. The Codex runs used
`codex exec -s read-only -C <repo> -c 'model_reasoning_effort="high"' --color never -o <rows.tsv> - < <prompt.md>`
(gpt-6-astra).

Historical labels in these records ("next", "proposed", "the small way") describe their own moment.
They do not set present work.
