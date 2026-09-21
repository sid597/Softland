Plan cut by: Claude Fable 5.1, max effort. Marked by: Codex, reasoning effort high.

You are doing a mechanical marking task on ONE file. Follow these rules exactly.

FILE: src/proposal/first-record-2026-09-20/research/clocks-ids-determinism.md (1106 lines)

Hard limits
- Read only that file. Do not open any other file: not the AGENTS.md reading list, not docs/,
  not loop/, not sources/, nothing else in the repository. Do not use the network.
- Do not create, edit, or delete any file. Do not run any command that writes. Your only
  output is your final message.
- Do not summarise or reword the file anywhere in your output, except in the short `note` column.

Background
A reader will read these line ranges of the file in full. Call this the KEPT text:
  1-135, 174-233, 267-330, 355-408, 439-485, 502-563, 596-643, 673-921, 1015-1106
The reader will NOT read these ranges unless you promote parts of them. Call this the CANDIDATE text:
  136-173, 234-266, 331-354, 409-438, 486-501, 564-595, 644-672, 922-1014

Task
Split every CANDIDATE range into blocks. A block is one paragraph, or one list item with its
continuation lines, or one heading line. Blank lines belong to the block before them. Every line
of every CANDIDATE range must fall in exactly one block. A block must not cross a range boundary.
Mark each block PROMOTE or DEFER.

DEFER is allowed only when one of these is true:
  D1  Every claim, reason, condition ("unless", "only if", "breaks when"), number, date, named
      system, named mechanism or format, quotation, regret, and disagreement in the block also
      appears in the KEPT text. You must give the KEPT line range or ranges where it appears. If
      even one such item is missing from the KEPT text, the block is PROMOTE.
  D2  The block is only a bibliographic entry or a list of them (title, author, link, date), or
      only a heading, with no claim about what the work shows.
In every other case, and whenever you are unsure, mark PROMOTE.

For PROMOTE give one tag naming what the block adds that the KEPT text lacks:
  P-claim  P-reason  P-condition  P-number  P-mechanism  P-quote  P-regret  P-disagreement  P-unsure

There is no target size. Do not try to make anything smaller. Promote everything that D1 and D2
do not clearly cover.

Output format
Your final message must be ONLY tab-separated rows. No header, no code fence, no prose before or
after. One row per block, in file order:
  start<TAB>end<TAB>PROMOTE or DEFER<TAB>tag<TAB>ref<TAB>note
- start, end: line numbers in the file, inclusive.
- tag: D1 or D2 for DEFER. One P- tag for PROMOTE.
- ref: for D1, the KEPT range or ranges, like 709-712 or 709-712,1020-1024. Always lo-hi, even
  for one line (709-709). For everything else a single hyphen.
- note: at most 12 words naming the block's subject. No tabs.

Method
Number the lines (nl -ba or cat -n). Read the whole file first, so you know the KEPT text before
you judge any block. Work range by range. Before answering, check that your rows cover every line
of every CANDIDATE range exactly once, and that every D1 ref lies inside the KEPT ranges.
