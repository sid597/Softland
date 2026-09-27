# Plan: the lookup from a fact to what stood on it (27 September 2026)

Written by the citation session (Opus 5.5, max effort) on branch
`citation-2026-09-27`, cut from `rig-2026-09-25`. Sid's brief, item 4:
"Build what the store is missing: a lookup from a fact to what stood on it.
Ids only, rebuildable from the log, respecting who may see what." Sid, mid
session: keep it in its own namespace, because the pointer session merges
it. Nothing here is a ruling; every choice marked **P-D** is a placeholder
taken as the simplest thing that can change later without touching a
record, and marked first-record where it would touch one.

**Revised after validation** (`PLAN_VALIDATION-citation.md`, major-fail on
the first version): the index is at act grain, not fact grain (F1); the
read is its own query (F1); the refusals are named where they are (F2,
F3); the rebuild is put pages only, with stale entries skipped at read
(F4, F5, F6); what the frame's promise holds under P-D1 is stated (F17);
the visibility question in the other direction goes to Sid (F19). The
section "Findings answered" says which.

Vocabulary as in the rest of the rig: "key" is a fact's key, "lock" an
encryption key. An *act* is one offer under one name; a *fact id* (fid) is
`[name idx]`. An act *stands on* facts: the offer's `:stood-on` map,
`{fid stamp}`, a part of the act, kept for a yes at `$$layers L :stood-on
name`. A *dependent* of fact F is an admitted act whose `:stood-on` names F.

## 1. What it is for

The frame's first promise: "When something you built on changes, you find
out. Always." The rig keeps, for each act, what it stood on (forward, on
the dependent's side). Nothing finds, for a given fact, what stood on it.
A tool woken by a change (the citation's marker) must find the acts that
stood on the old fact. This plan builds that lookup and nothing that
decides what happens when one is found: the walk and its default belong to
the tool (`PLAN-citation.md`), and the store writes nothing but the read
entry the exit already writes (P-D6, first-record: what the store writes
when it finds a dependent is among the handoff's questions that freeze at
the first kept record).

## 2. Where it lives: the dependent's side (P-D1)

One index field per one-owner layer, `:ix-dep`, on the layer's home task,
written in the same atomic event that admits the dependent act.
- No write leaves the admitting event; the other side (an index at the
  stood-on fact's home) would write on another task, possibly in the other
  store, after the answer, where "order between the two stores exists only
  through stood-on".
- Visibility comes from the layer: the entries sit in the dependent's
  layer, read through the one exit, whose `visible?` check governs that
  layer. A private dependent is never shown to the owner of what it stood
  on.
- The cost: finding every dependent of F store-wide means reading each
  layer that may hold one. A lookup reads one layer; the caller names the
  layers.

**What the promise holds under P-D1** (F17): "you find out" holds within
one layer. A dependent in another layer (Bob's citation standing on
`:softland`'s forms) is found only by a reader of that layer; nothing walks
it on its own. Under P-D3, after a layer is re-classed to the micro store
the lookup is refused for the layer's whole history, so the walk stops.

## 3. Schema and address (P-D2: act grain)

`rig.store.dependents/layer-fields`: `{:ix-dep (map-schema String entry
{:subindex-options {:track-size? false}})}`, merged into
`module/layers-schema`. The entry, `fixed-keys-schema`, ids only:

| field | class | what |
|---|---|---|
| `:on` | PersistentVector | the stood-on fact id F |
| `:on-stamp` | Long | F's stamp as the dependent carried it |
| `:act` | PersistentVector | the dependent act's name |
| `:stamp` | Long | the dependent act's stamp |
| `:n` | Long | how many facts the dependent act has |

Address, parts joined by U+0000: `fid-text(F) · hex16(stamp) ·
canonical(act)`, `fid-text([nm i])` the canonical EDN of `nm`, U+0000, `i`
as 8 hex digits (`rig.store.dependents` keeps its own copy of the form, so
it does not require `rig.store.reads`, which requires it). Unique per
(F, dependent act). For one F, dependents sort by stamp.

Act grain because stood-on is a part of the act (`env/act-parts`), not of
each fact: an act standing on m facts writes m entries, whatever its size.
(The first version wrote one entry per dependent fact and stood-on fact,
n × m, and quadratic under the marker's own workload, F1.)

## 4. Writes

`dependents/index-writes [nm stamp log stood-on]`: for an admitted act,
one `[:ix-dep address entry]` per stood-on fact, in the order of their
text. Total: nil when an input is not what the envelope's parse gives.

Call site, `gate/decide*`: for a yes, its result is appended to the
`:index-put` list `reads/index-writes` returns; nil is a throw, which
`decide` turns into its unrecorded `:gate-error` face refusal. The gate
event's existing block in `gate-event/write-decided>` (`(keypath *layer
*ix *ia) (termval *ie)`) writes the entries in the decision's one atomic
group with no read. `write-decided>` does not change.

Retries: the event reads the name's record first; a resend, or a replay
whose first attempt committed, goes the record path and writes nothing
here. A replay whose first attempt did not commit left nothing, decides
again, and one set of entries lands (F20). A promotion's landing into a
one-owner layer is decided by the same `decide`.

Volume: m writes per admitted act, bounded by what the offer carries; no
reads. A plain act stands on nothing and writes none.

Not written (P-D3): the micro gate's decisions. The micro store has no
`:ix-dep`.

## 5. The read: a pattern of the one exit, its own query

`[:dependents x]`, read through `rig.store.read-exit/read!` like every
pattern, so the entry is recorded, the moment applies and the exit's
visibility check governs. `x` is:
- a fact id `[name idx]`: what stood on that fact. Prefix `fid-text(x) ·`;
  the moment folds into the end (`prefix · hex16(m + 1)`), as `[:ek]`'s
  does.
- an act name `[layer class scheme id]`: what stood on any fact of that
  act. Prefix `canonical(name) ·`, ending at `prefix-end`; the page loop
  filters entries by stamp, as `[:k]`'s does.

`reads/parse-pattern` delegates to `dependents/parse-pattern`, and
`reads/bounds` to `dependents/bounds`. `read-pattern`'s `<<cond`, after
its refusals (a shared layer, visibility, the moment's kind, a `[:kv]`
refusal), hands a parsed lookup to the query `read-dependents`
(`rig.store.dependents-query`) by name, on the same task, with the moment
it computed from the home clock. That namespace may require
`rig.store.reads` because `reads` invokes it by name.

`read-dependents` pages over `:ix-dep` with the exit's own page loop
(`page-init`, `page-step`: the limit, the doubling pages, the scan budget
of 16 × (limit + 1) entries), groups the kept entries by dependent act
(`dependents/act-groups`, first appearance), and for each act whose record
is admitted by the moment reads `[:log act]` once (`subselect ALL`) and
opens every row through `open-row-with>`. Each row is the exit's point row
(`:fid :stamp :e :k :replaces :mark`, and `:value`, `:erased-at` or
`:unreadable`) with `:on`, the set of x's facts that act stood on. Keys
shown as opaque as every read shows them. The answer is the exit's
`pattern-answer`: the rows, `:matched` and the fingerprint over the
dependent facts' ids and stamps, and the mark.

An entry whose act has no admitted record by the moment is skipped, so an
entry the log no longer implies (after a restore) shows nothing (F4).

The limit counts the rows shown (review finding 4): the read stops before
an act that would pass it, or inside the first act when that act alone
passes it, and marks the answer `:partial`. Every loop yields. An
act-level lookup shows each act once.

The query checks what it is given before it reads (review finding 5), as
the shared queries invoked by name do: its arguments are `[layer for
parsed-pattern moment]`, and anything malformed is `{:refused :bad-read}`;
a layer of the shared kind is refused `:bad-pattern`, and a reader who may
not see the layer `:not-visible`. Called only through `read-pattern` it
never refuses, since that read checked the same first. Like every query
here, a direct call writes no read entry: the entry is the exit's, client
code in the rig, R5's read gateway in a kept store.

Refused, each as data before any index read:
- a shared layer, or a re-classed one: the shared path parses with
  `reads/parse-pattern` too, so `shared-reads/micro-pp` refuses the
  dependents kinds `:bad-pattern` (F2); `shared-pattern>` and `delta>`
  check it before reading.
- a standing read (P-D4): `read-delta` refuses `[:dependents x]`
  `:not-standing` before its opening, and `delta>` refuses a parsed one.
  The reason (F3): a standing read's deltas show entries through the
  exit's ordinary show step, which would show an act entry as a row with
  no value; a fact-level delta could otherwise work, since its entries are
  time-ordered in their prefix, and an act-level one could not. A live
  "what stood on F" is left for when a tool needs it.

## 6. Rebuild from the log (P-D5)

The index is a function of the log: for every admitted act, its
`:stood-on`, its stamp and its facts' count. The existing rebuild carries
it: the `:rebuild-put` page reads each yes act's `[:stood-on name]`
(`subselect ALL`) beside its rows, passes it to `put-act`, and
`put-page-writes` appends `dependents/index-writes` for each act (an act
it cannot index adds nothing, F6). The page's bound counts those stood-on
entries with the rows (F5). `rebuild!` runs put pages first, so it
rebuilds `:ix-dep` too.

No sweep: puts never leave a window with entries missing, and a stale
entry is skipped at read (section 5), so nothing needs deleting for the
answers to be right. `:rebuild-sweep` over `:ix-dep` is refused `:bad-op`;
`:drop` accepts the field (a test-only op) so a test can empty it and
rebuild it.

`dependents/implied [acts]` gives what a finished rebuild leaves, for
tests.

## 7. What it does not do

- It finds nothing across layers by itself (section 2).
- It writes no notice when a stood-on fact is replaced (P-D6).
- It knows no key: nothing here names a citation, a form or a tool.

## 8. Costs

- Admission: m entry writes an act (m stood-on facts), no reads.
- A fact-level lookup: one seek and an iteration over its entries; then,
  per dependent act, one seek on its record and one on its log with an
  iteration over its facts, and the open's seeks per row.
- An act-level lookup scans every entry of every fact of the act, filtered
  by stamp; each dependent act is read once however many facts of x it
  stood on.

## 9. Tests (`test/rig/store/dependents_test.clj`, one in-process cluster)

1. An act standing on F: `[:dependents F]` shows each of its facts with its
   value and `:on #{F}`; its entry is one, ids only; an act standing on
   nothing adds nothing.
2. The moment: a read as of a stamp before the dependent shows nothing.
3. The act-level lookup: dependents of any fact of the act, each act once,
   `:on` naming which; three facts on two facts write two entries.
4. Visibility: Bob's act in Bob's layer standing on Alice's fact is not in
   Alice's lookup, and her read of Bob's layer is refused `:not-visible`.
5. A resend under the same name adds no entry; a refused act adds none.
6. Rebuild: what the gate wrote equals what the log implies; drop every
   entry, run the rebuild, and the same entries return.
7. A forgotten dependent value shows erased, its ids still found; the
   forget act stood on what it forgot.
8. Refusals: a malformed x; a standing read; the micro store's parse.
9. The read entry records the pattern and the matched dependents.

## 10. Placeholders

- P-D1: the dependent's side; a lookup reads one layer; the promise holds
  within one layer.
- P-D2: act grain, one entry an act a stood-on fact.
- P-D3: the stream store only; shared and re-classed layers refuse the
  pattern.
- P-D4: no standing read of dependents.
- P-D5: rebuilt by put pages; stale entries skipped at read; no sweep.
- P-D6 (first-record): the store writes nothing on finding a dependent but
  the read entry.
- The entry's fields and the address are index shapes, rebuildable, so
  neither touches a record. The pattern's form `[:dependents x]` and the
  rows' `:on` are recorded in read entries (the pattern) and seen by
  readers, so they become first-record at the first kept record.

## 11. Findings answered (from `PLAN_VALIDATION-citation.md`)

- F1: act grain (sections 3 to 6).
- F2: `micro-pp` refuses the dependents kinds (section 5).
- F3: the reason and both sites, `read-delta` and `delta>` (section 5).
- F4: no sweep; put-only rebuild through `rebuild!`; stale entries
  skipped at read (sections 5, 6). The validator's preferred fix was a
  sweep judge; skipping at read makes the answers right without deleting,
  and a sweep can be added later without touching a record.
- F5: stood-on entries count toward the put page's bound.
- F6: `index-writes` is total; an act it cannot index adds nothing to a
  rebuild page, and is `:gate-error` at the gate.
- F17: section 2.
- F19, for Sid: the lookup shows an act's `:on` to anyone who can read the
  act's layer. A promotion's landing stands on the source fact and the
  crossing, whose name carries the source layer, so a reader of the target
  could confirm a guess of the source layer by looking up
  `[:dependents [source-layer nil :crossing uuid]]`. Either a promotion's
  provenance is visible to the target's readers, or `:on` is filtered to
  facts in layers the reader can see. Not built either way.
- F20: section 4.
