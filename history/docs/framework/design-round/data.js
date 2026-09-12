// data.js — stand-in dataset for the block-view explorations.
// One human↔agent coding session ("exploratory refactor of ingest/src/blockize.ts"),
// page 1 of a longer conversation. Shapes below are the contract; swap content
// for real pipeline output later.
//
// Exports:
//   meta   — conversation-level facts (page-complete? false, counts, key)
//   turns  — [{ n, n2, evFrom, evTo, user:{block,chunks}, respEvFrom, respEvTo, response:[blocks], words, blockCount }]
//   allBlocks, byId, turnOf — flat helpers
//
// Block: { id, sid, event, part, idx, type, actor, text, words, lines }

const KEY = 'wrk_9f31c2';
const pad = (n) => String(n).padStart(6, '0');

// ---------- deterministic rng ----------
function mulberry32(seed) {
  let a = seed | 0;
  return function () {
    a = (a + 0x6D2B79F5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
const R = mulberry32(0xC0FFEE);
const ri = (n) => Math.floor(R() * n);
const wc = (t) => t.trim().split(/\s+/).filter(Boolean).length;

// ---------- corpus: agent thinking (grouped so paragraphs stay on-topic) ----------
const THINK = {
  splitter: [
    'The splitter is a single forward scan with three states — prose, fence, list — and the fence state can be entered twice without exiting when the closing backticks carry a language tag, which would explain the merged-paragraph fixture.',
    'splitUserMessage does double duty: it yields boundaries and, as a side effect, advances the running offset cursor that emitBlocks reads later, so the two cannot be reordered casually.',
    'A blank line inside a fence is currently treated as a paragraph boundary, which is flatly wrong for any pasted stack trace.',
    'The list state is only reachable from prose, never from fence, so a bulleted list directly under a code block gets glued to the code above it.',
    'Two-pass would mean: pass one tokenizes into spans with extents, pass two folds spans into blocks; boundaries become data instead of control flow.',
    'The scanner keeps three mutable cursors in closure scope; none are reset between messages, they just happen to end at the right values most of the time.',
    'Nothing in the current shape prevents a zero-length block, and I count two guards downstream that quietly drop them — load-bearing sloppiness.',
    'Windows line endings reach the scanner unnormalized on one path (fixtures) and normalized on the other (live ingest), which is why the bug only reproduces in tests.',
    'If the state machine stays, exiting fence must key on the fence character run length, not on the literal three-backtick string.',
  ],
  ids: [
    'Id construction is string concatenation in at least four call sites, each with its own zero-padding helper; two pad to six digits, one pads to five, and one does not pad at all and has never been hit.',
    'parseBlockId and makeBlockId do not round-trip: parse tolerates a missing part segment, make always emits one.',
    'The conversation key is threaded through nine function signatures just to reach the concat at the bottom; a bound factory would delete eight of those parameters.',
    'Everything downstream treats ids as opaque strings except the retry deduper, which slices character ranges out of them — that is the real coupling to break.',
    'block-v0 is hardcoded in three files; minting a v1 would be an easy grep, but the fixtures encode v0 in about six hundred places.',
    'Stability constraint restated: same input events must yield byte-identical ids, so the refactor can move code but must not touch pad widths, separator choice, or segment order.',
    'The event counter increments on emit, not on receipt, so a crash between receipt and emit reuses an event number after restart — dedupe currently papers over this.',
    'ids.ts should own format, parse, and validation, and be the only file allowed to know there are colons in there.',
    'There is a latent off-by-one: part is written as 00 everywhere today, but the writer reserves it for attachment fan-out and the reader assumes it is literally zero.',
  ],
  offsets: [
    'Offsets are UTF-16 code units because that is what slice gives you for free; the annotation tooling counts grapheme clusters; both are internally consistent and mutually wrong.',
    'A single emoji in a user message shifts every subsequent offset by one from the annotators\u2019 point of view, and the drift compounds per emoji.',
    'Normalizing to NFC on ingest would change lengths for decomposed sequences already in the store, so whatever we pick has to be versioned, not silently corrected.',
    'CRLF normalization currently happens in normalize.ts for live events and in a test helper for fixtures, and the two implementations disagree about a lone CR.',
    'The cheap fix is to store both unit systems side by side per block; storage cost is two integers, migration cost is a backfill job.',
    'computeOffsets walks the raw string once per block instead of once per message, which makes it quadratic on chunk-heavy messages — invisible until someone pastes a long log.',
    'Grapheme segmentation via Intl.Segmenter is available in our node target; the question is not capability, it is which numbers are already persisted.',
    'Any fix that changes existing persisted offsets is a migration, not a bug fix, and needs the annotation team in the room.',
    'fixtures/session-047.json is the one with the thread emoji; it should become the canonical regression test for whatever we decide.',
  ],
  tests: [
    'Coverage is honest about the happy path and silent about everything interesting: no fixture has a fence with a language tag, none has CRLF, none has an empty paragraph run.',
    'The golden files encode today\u2019s behavior, bugs included, so a green suite after the refactor proves preservation, not correctness.',
    'Half the specs assert on serialized JSON strings rather than structures, which means any key-order change reads as a failure.',
    'Goldens should split into format goldens (must stay byte-identical) and behavior goldens (may change with an approved diff).',
    'The suite takes ninety seconds because every spec re-parses the full fixture corpus; a shared parsed cache would cut that to seconds.',
    'There is exactly one property-based test in the repo and it is skipped with a comment that says \u201cflaky, revisit\u201d, dated two years ago.',
    'session-031 is the fixture that shouts in all caps; it is also the only one exercising a five-tool-call run, so it earns its keep.',
    'A regenerated golden that differs only in trailing newline is the tell that CRLF normalization diverged again.',
    'We need a fixture that is one enormous thinking block, because the chunker\u2019s cap logic has never once executed outside production.',
  ],
  emitter: [
    'emitBlocks batches by count (fifty) but not by bytes, so one pasted log can produce a batch the store refuses, and the retry path re-sends the whole batch.',
    'The wire format is frozen by three consumers; the emitter can change how it batches but not what a record looks like.',
    'store.write is awaited inside a for-loop — sequential round trips — and the comment above it says TODO: parallelize, dated before the store had a bulk endpoint.',
    'Backpressure is a boolean; the emitter either runs or sleeps 500ms, and the constant is duplicated in two files with different values.',
    'Partial batch failure drops the failed records into a retry queue keyed by block id, which is where the id-slicing coupling comes from.',
    'The batch flag on emitBlocks defaults on in production and off in tests, so tests exercise a code path production never runs.',
    'If pass one of the splitter owns extents, the emitter can stream blocks as they fold instead of accumulating per message.',
    'Nothing fsyncs; a crash mid-batch relies on the deduper to make re-ingest idempotent, which it mostly is.',
  ],
  dedupe: [
    'dedupeRetries keys on a sliced substring of the block id — characters 24 through 41 — which is the event and part segments by accident of today\u2019s key length.',
    'The dedupe window is an in-memory Map with no eviction; long sessions grow it unboundedly, and a comment from the original author admits it.',
    'Retries arrive with identical ids but occasionally different text when upstream retried a partially-flushed event; current policy is last-write-wins, silently.',
    'Tombstones exist so a dedup decision survives restart; deleting them would reintroduce the double-emit bug from the January incident.',
    'A content hash next to the id would make last-write-wins detectable instead of silent, at the cost of one sha1 per block.',
    'The deduper is the only component that parses ids instead of treating them as opaque; fixing that removes the last blocker to changing key length.',
    'Idempotency is tested by replaying fixture 12 twice and diffing store writes — a good test that exists only in a shell script nobody runs.',
  ],
  plan: [
    'Sequencing: extract ids.ts first because it is pure motion, then normalize newlines at the single entry point, then the two-pass splitter behind a flag, then the emitter.',
    'Each step lands as its own PR with goldens regenerated at most once, and the regeneration diff reviewed line by line.',
    'The riskiest change is the splitter; the flag lets both implementations run in parallel in staging with emitted blocks diffed for a week.',
    'Offset semantics stay untouched in this pass; storing both unit systems is a later migration behind the annotation team\u2019s sign-off.',
    'Rollback story: every PR is behind either a flag or pure refactor, so revert is git-level, no data migration in the critical path.',
    'The 900-line file ends this week as four files under 250 lines, or the refactor has failed at its own goal.',
    'Grep count as progress metric: references to makeBlockId outside ids.ts should go from thirty-one to zero.',
    'Anything that changes bytes on the wire is out of scope, full stop; that constraint is what makes this a week and not a quarter.',
  ],
};
const THINK_KEYS = Object.keys(THINK);

// ---------- corpus: agent prose / lists ----------
const PROSE = {
  explain: [
    'The splitter is a three-state scanner, and the fence state is the buggy one: a language-tagged closing fence fails the exact-match check and the scanner stays in fence mode for the rest of the message.',
    'Ids are built by string concatenation in four separate places, with three different padding behaviors.',
    'Offsets are counted in UTF-16 code units end to end; the annotation side counts graphemes, which is the off-by-one your team keeps seeing.',
    'CRLF handling diverges between live ingest and the fixture loader — that is why this only reproduces in tests.',
    'The event counter increments on emit rather than receipt, so restarts can reuse an event number; the deduper currently absorbs this.',
    'emitBlocks batches by record count, not bytes, and retries whole batches on partial failure.',
  ],
  report: [
    'Read the full module top to bottom; the map below covers every construct that touches ids or offsets.',
    'The grep results agree with the architecture note: nothing outside dedupe.ts parses an id.',
    'All thirty-one call sites are accounted for; twenty-eight are mechanical replacements, three need thought.',
    'The suite is green except the four goldens we expected to move; the diff on each is exactly the fence fix.',
    'Benchmarks put the two-pass splitter within two percent of single-pass on the p50, and eleven percent faster on the pathological fixture.',
    'The write-up is in docs/blockize.md; it is deliberately one page.',
  ],
  propose: [
    'Recommendation: extract ids.ts first — pure code motion, and it removes eight threaded parameters on its own.',
    'Next, normalize newlines at the single ingest entry point and delete the fixture-loader copy.',
    'The splitter rewrite should sit behind BLOCKIZE_TWO_PASS so staging can diff both implementations on live traffic.',
    'Goldens should split into format goldens and behavior goldens before anything gets regenerated.',
    'Keep the tombstones; add a content hash next to the id so silent last-write-wins becomes observable.',
  ],
  caveat: [
    'One caveat: fixture session-047 has a real emoji offset drift, so its golden will move for a reason unrelated to this change.',
    'Note that the batch flag means tests currently exercise a path production never runs — worth fixing before the emitter work.',
    'Offset semantics are untouched; that is a migration conversation, not a refactor.',
    'The CRLF fixtures are quarantined, not fixed; the loader now rejects rather than normalizes.',
  ],
};
const LISTS = {
  findings: [
    'splitUserMessage — src/blockize.ts:141 — owns paragraph boundaries and mutates the shared offset cursor',
    'makeBlockId — four call sites: blockize.ts:412, blockize.ts:583, emit/store.ts:77, dedupe.ts:31',
    'fence state entered at blockize.ts:198; exit check is an exact "```" match — language tags never exit',
    'normalizeNewlines exists twice: src/normalize.ts:12 and test/helpers/load.ts:44 — they disagree on lone CR',
    'computeOffsets is O(message × blocks) — quadratic for chunk-heavy messages',
    'dedupe window: unbounded Map; comment at dedupe.ts:19 admits it',
    'batch flag: default true in prod config, false in vitest setup — divergent paths',
    'event counter increments in emitBlocks, not on receipt — restart can reuse an event number',
  ],
  steps: [
    'extract id format/parse/validate into src/ids.ts — pure motion, no behavior change',
    'single CRLF normalization at the ingest entry point; delete the fixture-loader copy',
    'two-pass splitter behind BLOCKIZE_TWO_PASS; staging diff for a week',
    'split goldens: format vs behavior; regenerate once, review the diff line by line',
    'emitter: batch by bytes as well as count; retry records, not batches',
    'add fixtures/session-047.json as the offsets regression anchor',
    'content hash beside id in the retry queue; keep tombstones',
    'delete the skipped property test or make it run — two years of "flaky, revisit" is a decision',
  ],
  triage: [
    'split.spec.ts › fences › language-tagged close — real target; fails on main too with the new fixture',
    'goldens/session-012 — stale: diff is exactly the fence fix, safe to regenerate',
    'goldens/session-047 — moves for the emoji reason; hold for annotation sign-off',
    'emit.spec.ts › batch retry — flaky: depends on Map iteration order, unrelated',
    'ids.spec.ts › round-trip — real: parse tolerates a missing part segment, make does not',
  ],
  risks: [
    'byte-identical ids — guarded by format goldens plus the round-trip property test',
    'wire format freeze — no record shape changes anywhere in this plan',
    'offset semantics — explicitly out of scope; both-units storage is a later migration',
    'staging diff window — one week minimum before the flag flips in prod',
    'rollback — every PR is a flag or pure motion; revert is git-level',
  ],
  todos: [
    'dedupe.ts:19 — TODO eviction; unbounded Map since 2023',
    'emit/store.ts:78 — TODO parallelize; the bulk endpoint exists now',
    'blockize.ts:204 — HACK exact-match fence close; the bug we found',
    'blockize.ts:566 — TODO zero-length block guard, duplicated downstream twice',
    'normalize.ts:12 — FIXME lone CR treated as newline here, stripped in the test helper',
    'offsets.ts:31 — TODO quadratic walk; cache per message',
    'emit/batch.ts:9 — TODO backpressure constant duplicated (500 here, 250 in store)',
    'dedupe.ts:31 — FIXME slices character ranges out of the id',
  ],
};

// ---------- composers ----------
function takeFrom(group, used, count) {
  const avail = group.filter((s) => !used.has(s));
  const src = avail.length >= count ? avail : group;
  const pool = src.slice();
  const out = [];
  for (let i = 0; i < count && pool.length; i++) {
    const k = ri(pool.length);
    out.push(pool[k]);
    pool.splice(k, 1);
  }
  out.forEach((s) => used.add(s));
  return out;
}
function thinkText(target, openers, closers) {
  const used = new Set();
  const paras = (openers || []).slice();
  let w = paras.reduce((a, p) => a + wc(p), 0);
  const closeW = (closers || []).reduce((a, p) => a + wc(p), 0);
  let ki = ri(THINK_KEYS.length);
  let guard = 0;
  while (w < target - closeW && guard++ < 40) {
    const g = THINK[THINK_KEYS[ki % THINK_KEYS.length]];
    ki += 1 + ri(2);
    const sents = takeFrom(g, used, 3 + ri(3));
    if (!sents.length) break;
    const p = sents.join(' ');
    paras.push(p);
    w += wc(p);
  }
  if (closers) paras.push(...closers);
  return paras.join('\n\n');
}
function proseBlocks(n, groups) {
  const used = new Set();
  const out = [];
  for (let i = 0; i < n; i++) {
    const g = PROSE[groups[i % groups.length]];
    out.push(takeFrom(g, used, 1 + (R() < 0.35 ? 1 : 0)).join(' '));
  }
  return out;
}
function listBlocks(n, group) {
  const used = new Set();
  const out = [];
  for (let i = 0; i < n; i++) out.push(takeFrom(LISTS[group], used, 1)[0]);
  return out;
}

// ---------- fake repo output builders ----------
const SRC = [
  'export function splitUserMessage(raw: string, ctx: SplitCtx): Chunk[] {',
  '  const out: Chunk[] = [];',
  '  let state: ScanState = "prose";',
  '  let cursor = 0; let fenceRun = 0; let para = 0;',
  '  for (let i = 0; i < raw.length; i++) {',
  '    if (state === "fence") {',
  '      // NOTE: exact-match close; language-tagged fences never exit (#812)',
  '      if (raw.startsWith("```", i) && raw[i + 3] === "\\n") state = "prose";',
  '      continue;',
  '    }',
  '    if (isBlankLine(raw, i)) {',
  '      out.push(fold(raw, cursor, i, para++));',
  '      cursor = i + 1; // offsets: utf-16 units, see offsets.ts',
  '    }',
  '  }',
  '  ctx.offset += cursor; // shared mutable cursor — emitBlocks reads this',
  '  return out;',
  '}',
  '',
  'function makeBlockId(key: string, ev: number, part: number, idx: number) {',
  '  return "du:chat:" + key + ":block-v0:" + pad6(ev) + ":" + pad2(part) + ":" + pad6(idx);',
  '}',
  '',
  'export async function emitBlocks(blocks: BlockRecord[], opts: EmitOpts) {',
  '  const batches = chunk(blocks, 50); // count, not bytes — see #791',
  '  for (const b of batches) {',
  '    await store.write(b); // TODO: parallelize (bulk endpoint exists now)',
  '    eventCounter += 1;    // increments on emit, not receipt',
  '  }',
  '}',
  '',
  'export function dedupeRetries(rec: BlockRecord): boolean {',
  '  const k = rec.id.slice(24, 41); // event+part by accident of key length',
  '  if (window.has(k)) return false; // last-write-wins, silently',
  '  window.set(k, sha1?  hash(rec.text) : true); // Map, no eviction',
  '  return true;',
  '}',
];
function excerpt(start, n) {
  const L = [];
  for (let i = 0; i < n; i++) {
    const src = SRC[i % SRC.length];
    L.push(String(start + i).padStart(4, ' ') + '\u2502 ' + src);
  }
  return L.join('\n');
}
const GREP_PATHS = ['src/blockize.ts', 'src/emit/store.ts', 'src/emit/batch.ts', 'src/dedupe.ts', 'src/normalize.ts', 'src/offsets.ts', 'src/split/fences.ts', 'test/helpers/load.ts', 'src/__tests__/ids.spec.ts', 'src/__tests__/split.spec.ts'];
const GREP_FRAGS = [
  'const id = makeBlockId(key, ev, part, idx);',
  'return "du:chat:" + key + ":block-v0:" + pad6(ev) + ":" + pad2(part) + ":" + pad6(idx);',
  'const [, , , , ev, part, idx] = id.split(":");',
  'if (!id.startsWith("du:chat:")) throw new BadId(id);',
  'const k = rec.id.slice(24, 41);',
  'expect(makeBlockId("k", 12, 0, 3)).toMatchInlineSnapshot();',
  'ids.push(pad5(ev) + ":" + idx); // FIXME pad width',
  'raw = raw.replace(/\\r\\n/g, "\\n"); // fixture loader copy',
  'ctx.offset += chunk.text.length; // utf-16 units',
  'emitBlocks(fold(spans), { batch: cfg.batch ?? true });',
];
function grepHits(n) {
  const L = [];
  for (let i = 0; i < n; i++) {
    const p = GREP_PATHS[ri(GREP_PATHS.length)];
    const ln = 20 + ri(880);
    L.push(p + ':' + ln + ':  ' + GREP_FRAGS[(i + ri(3)) % GREP_FRAGS.length]);
  }
  return L.join('\n');
}
const TEST_NAMES = [
  'keeps fences intact across blank lines', 'splits on double newline', 'preserves list runs as separate blocks',
  'emits stable ids for identical input', 'round-trips the part segment', 'pads event to six digits',
  'elides the conversation key in short form', 'counts utf-16 units per chunk', 'offsets are monotonic within a message',
  'normalizes CRLF at the entry point', 'rejects a lone CR', 'batches by record count',
  'retries only failed records', 'tombstones survive restart', 'dedupes identical retries within window',
  'drops zero-length blocks with a warning', 'orders sibling blocks deterministically', 'folds spans into blocks in one pass',
  'language-tagged fence exits fence state', 'fence run length longer than three', 'sub-chunk indices are contiguous',
  'parent message text equals joined chunks', 'empty message yields no blocks', 'thinking blocks are capped at limit',
];
function testLog(bigFailures) {
  const L = [];
  L.push('$ pnpm vitest run --reporter=verbose');
  L.push('');
  L.push(' RUN  v1.6.0  /work/ingest');
  L.push('');
  const suites = [
    ['split.spec.ts', 21, bigFailures ? 2 : 0], ['ids.spec.ts', 14, bigFailures ? 1 : 0],
    ['offsets.spec.ts', 12, bigFailures ? 1 : 0], ['emit.spec.ts', 13, 0],
    ['dedupe.spec.ts', 10, 0], ['normalize.spec.ts', 9, bigFailures ? 1 : 0],
  ];
  let ti = 0; let failNames = [];
  for (const [file, count, fails] of suites) {
    L.push(' \u276F src/__tests__/' + file + '  (' + count + ' tests' + (fails ? ' | ' + fails + ' failed' : '') + ')  ' + (180 + ri(700)) + 'ms');
    for (let i = 0; i < count; i++) {
      const name = TEST_NAMES[ti++ % TEST_NAMES.length] + '  (session-' + String(1 + ri(62)).padStart(3, '0') + ')';
      const failed = i < fails;
      if (failed) failNames.push(file + ' > ' + name);
      L.push('   ' + (failed ? '\u00D7' : '\u2713') + ' ' + name + '  ' + (1 + ri(90)) + 'ms');
    }
    if (file === 'split.spec.ts') L.push('   stdout | fixtures: parsed 62 sessions in 1401ms (cache miss)');
    if (file === 'emit.spec.ts') L.push('   stdout | store: using memory driver, batch=false');
    L.push('');
  }
  if (bigFailures) {
    for (const fn of failNames.slice(0, 3)) {
      L.push(' FAIL  ' + fn);
      L.push('   AssertionError: expected 4 blocks, received 3');
      L.push('     - Expected');
      L.push('     + Received');
      L.push('       [');
      L.push('         "\u2026:000012:00:000001",');
      L.push('         "\u2026:000012:00:000002",');
      L.push('     -   "\u2026:000012:00:000003",');
      L.push('       ]');
      L.push('     \u276F src/__tests__/' + fn.split(' > ')[0] + ':' + (40 + ri(200)));
      L.push('     \u276F fold src/blockize.ts:566');
      L.push('     \u276F splitUserMessage src/blockize.ts:141');
      L.push('');
    }
  }
  const total = suites.reduce((a, s) => a + s[1], 0);
  const failed = failNames.length;
  L.push(' Test Files  ' + (failed ? '4 failed | 2 passed' : '6 passed') + ' (6)');
  L.push('      Tests  ' + (failed ? failed + ' failed | ' + (total - failed) + ' passed' : total + ' passed') + ' (' + total + ')');
  L.push('   Duration  ' + (bigFailures ? '91.44s' : '88.02s') + ' (setup 1.2s, collect 61.8s, tests 25.1s)');
  return L.join('\n');
}
const TREE_OUT = [
  'src/', '  blockize.ts        882 lines', '  ids.ts              (does not exist yet)', '  normalize.ts         64 lines',
  '  offsets.ts          118 lines', '  dedupe.ts            97 lines', '  split/', '    paragraphs.ts     212 lines',
  '    fences.ts          88 lines', '  emit/', '    store.ts          161 lines', '    batch.ts           74 lines',
  '  __tests__/           6 spec files', 'fixtures/              62 session-*.json (goldens)', 'docs/                  (empty)',
].join('\n');
const DIFF_IDS = [
  'diff --git a/src/ids.ts b/src/ids.ts', 'new file mode 100644', '--- /dev/null', '+++ b/src/ids.ts',
  '@@ -0,0 +1,38 @@', '+// ids.ts — the only module allowed to know the id format.',
  '+export const BLOCK_V0 = "block-v0";', '+const pad6 = (n: number) => String(n).padStart(6, "0");',
  '+const pad2 = (n: number) => String(n).padStart(2, "0");', '+',
  '+export function makeBlockId(key: string, ev: number, part: number, idx: number) {',
  '+  return "du:chat:" + key + ":" + BLOCK_V0 + ":" + pad6(ev) + ":" + pad2(part) + ":" + pad6(idx);',
  '+}', '+', '+export function parseBlockId(id: string): ParsedId {', '+  const seg = id.split(":");',
  '+  if (seg.length !== 7) throw new BadId(id); // was: tolerated 6', '+  return { key: seg[2], ev: +seg[4], part: +seg[5], idx: +seg[6] };',
  '+}', 'diff --git a/src/blockize.ts b/src/blockize.ts', '--- a/src/blockize.ts', '+++ b/src/blockize.ts',
  '@@ -409,9 +409,7 @@', '-  const id = "du:chat:" + key + ":block-v0:" + pad6(ev) + ":" + pad2(part) + ":" + pad6(idx);',
  '+  const id = makeBlockId(key, ev, part, idx);', '@@ -580,8 +578,7 @@',
  '-  out.push("du:chat:" + key + ":block-v0:" + pad5(ev) + ":00:" + i); // never hit',
  '+  out.push(makeBlockId(key, ev, 0, i));', 'diff --git a/src/emit/store.ts b/src/emit/store.ts',
  '@@ -74,7 +74,7 @@', '-  const rid = "du:chat:" + key + ":block-v0:" + pad6(ev) + ":00:" + pad6(i);',
  '+  const rid = makeBlockId(key, ev, 0, i);', 'diff --git a/src/dedupe.ts b/src/dedupe.ts', '@@ -28,7 +28,8 @@',
  '-  const k = rec.id.slice(24, 41);', '+  const p = parseBlockId(rec.id);', '+  const k = p.ev + ":" + p.part;',
].join('\n');
const DIFF_NORM = [
  'diff --git a/src/normalize.ts b/src/normalize.ts', '--- a/src/normalize.ts', '+++ b/src/normalize.ts',
  '@@ -9,11 +9,13 @@', ' export function normalizeNewlines(raw: string): string {',
  '-  return raw.replace(/\\r\\n/g, "\\n");', '+  // single entry point: CRLF and lone CR both fold to LF',
  '+  return raw.replace(/\\r\\n?/g, "\\n");', ' }', 'diff --git a/test/helpers/load.ts b/test/helpers/load.ts',
  '@@ -41,9 +41,7 @@', '-  raw = raw.replace(/\\r\\n/g, "\\n"); // local copy, disagreed on lone CR',
  '-  if (raw.includes("\\r")) warn("lone CR left in fixture");', '+  // normalization now happens in normalize.ts at the entry point',
  '@@ -63,6 +61,8 @@', '+  if (raw.includes("\\r")) throw new Error("fixture contains CR — quarantine it");',
].join('\n');
const DIFF_HASH = [
  'diff --git a/src/dedupe.ts b/src/dedupe.ts', '--- a/src/dedupe.ts', '+++ b/src/dedupe.ts', '@@ -29,10 +29,14 @@',
  '   const p = parseBlockId(rec.id);', '   const k = p.ev + ":" + p.part;', '+  const h = sha1(rec.text);',
  '   if (window.has(k)) {', '-    return false; // last-write-wins, silently', '+    if (window.get(k) !== h) metrics.count("dedupe.text_mismatch");',
  '+    return false; // still last-write-wins, but observable now', '   }', '-  window.set(k, true);',
  '+  window.set(k, h); // tombstones kept — see January incident', '   return true;',
].join('\n');
const BENCH_OUT = [
  '$ node bench/splitter.mjs --runs 200', 'single-pass   p50 412\u00B5s   p95 2.11ms   max 41.0ms (session-058, pasted log)',
  'two-pass      p50 420\u00B5s   p95 1.94ms   max 36.4ms', 'delta         p50 +1.9%   p95 -8.1%    max -11.2%',
  'allocs/msg    single 118   two-pass 96', 'verdict: within noise on p50; wins on pathological inputs',
].join('\n');
const GIT_OUT = [
  '$ git log --oneline -3 && git push origin refactor/ids-module', '9f31c2e ids: extract format/parse into src/ids.ts (pure motion)',
  '4d08a11 normalize: single CRLF entry point; quarantine CR fixtures', 'b7e2f90 goldens: regenerate format goldens (reviewed)',
  'To github.com:acme/ingest.git', '   e4c19d2..9f31c2e  refactor/ids-module -> refactor/ids-module',
].join('\n');
const REGEN_OUT = [
  '$ pnpm goldens:regen --filter format', 'regenerating 62 fixtures\u2026', '  unchanged: 58', '  moved: 4 (session-012, session-019, session-031, session-047)',
  '  wrote fixtures/.regen-report.json', 'review the diff before committing — regen is not approval',
].join('\n');
const TODO_OUT = (function () {
  const L = ['$ grep -rn "TODO\\|FIXME\\|HACK" src/ --include "*.ts"'];
  const hits = [
    'src/dedupe.ts:19:  // TODO(2023): eviction — this Map grows forever',
    'src/emit/store.ts:78:  // TODO: parallelize (bulk endpoint exists now)',
    'src/blockize.ts:204:  // HACK: exact-match fence close, see #812',
    'src/blockize.ts:566:  // TODO: zero-length guard duplicated in store + dedupe',
    'src/normalize.ts:12:  // FIXME: lone CR — disagrees with test helper',
    'src/offsets.ts:31:  // TODO: quadratic walk, cache per message',
    'src/emit/batch.ts:9:  // TODO: backpressure constant duplicated (500 vs 250)',
    'src/dedupe.ts:31:  // FIXME: slicing char ranges out of the id',
    'src/split/fences.ts:44:  // TODO: fence run length > 3',
    'src/split/paragraphs.ts:88:  // TODO: blank line inside fence is not a boundary',
    'src/emit/batch.ts:41:  // HACK: sleep(500) as backpressure',
    'src/offsets.ts:77:  // FIXME: utf-16 vs graphemes — annotation team',
  ];
  return L.concat(hits).join('\n');
})();
const DOCS_OUT = [
  '$ wc -l docs/blockize.md && head -6 docs/blockize.md', '  84 docs/blockize.md', '# blockize — architecture note (2026-07)',
  '', 'Raw session events -> normalize -> split (two-pass) -> ids -> emit.', 'Frozen: wire format, id bytes. Fluid: batching, dedupe keys, caching.',
].join('\n');

// ---------- the 30 turns ----------
// u: pinned user paragraphs. r: response segments in order.
const S = (k, extra) => Object.assign({ k }, extra);
const SPEC = [
  { // 1
    u: ['ok so. the ingest repo is a mess and I finally have a week to deal with it. the thing I care about is src/blockize.ts — it\u2019s ~900 lines and it does everything: splits raw session events into blocks, assigns ids, computes offsets, dedupes retries, emits to the store. nobody fully understands it anymore, including me, and I wrote most of it.',
      'I don\u2019t want you to change anything yet. I want an exploration pass first: read the module, map what calls what, find every place that constructs or parses a block id, and get the shape of the thing into your head. treat it like an archaeology dig\u2026 layers, not features.',
      'constraints: the emitted block shape is consumed by three downstream jobs so the wire format is frozen. ids are stable and MUST stay byte-identical. tests are\u2026 partial. ask before running anything destructive.'],
    r: [S('think', { w: 260 }), S('prose', { n: 2, g: ['explain', 'propose'] }), S('list', { n: 4, g: 'steps' }),
        S('tool', { t: 'ls(path="src/", depth=2, sizes=true)' }), S('res', { t: TREE_OUT })],
  },
  { // 2
    u: ['start with blockize.ts top to bottom. don\u2019t skim — I want you to actually read it, and tell me what you notice that I didn\u2019t put in the brief.'],
    r: [S('tool', { t: 'read_file(path="src/blockize.ts", offset=0, limit=240)' }), S('res', { t: excerpt(1, 58) }),
        S('tool', { t: 'read_file(path="src/blockize.ts", offset=240, limit=240)' }), S('res', { t: excerpt(241, 44) }),
        S('think', { w: 210 }), S('prose', { n: 2, g: ['report', 'explain'] }), S('list', { n: 5, g: 'findings' })],
  },
  { // 3
    u: ['what calls splitUserMessage? everything or just the ingest path?'],
    r: [S('think', { w: 120 }), S('tool', { t: 'grep(pattern="splitUserMessage", path="src/", include="*.ts")' }), S('res', { t: grepHits(9) }),
        S('prose', { n: 2, g: ['report', 'explain'] }), S('list', { n: 3, g: 'findings' })],
  },
  { // 4
    u: ['and emitBlocks? same question basically\u2026 also check who reads ctx.offset because I have a bad feeling about that cursor'],
    r: [S('tool', { t: 'grep(pattern="emitBlocks(", path="src/")' }), S('res', { t: grepHits(7) }),
        S('tool', { t: 'grep(pattern="ctx.offset", path="src/")' }), S('res', { t: grepHits(6) }),
        S('prose', { n: 1, g: ['report'] }), S('list', { n: 4, g: 'findings' })],
  },
  { // 5 — the grep spree
    u: ['ok now the big one. map EVERY place a block id is constructed or parsed. string concat, template, slice, split, regex, whatever. I want one module owning the format by friday and I don\u2019t want to discover a fifth call site in prod.'],
    r: [S('think', { w: 300 }),
        S('tool', { t: 'grep(pattern="du:chat:", path="src/")' }), S('tool', { t: 'grep(pattern="block-v0", path="src/")' }), S('tool', { t: 'grep(pattern="makeBlockId|parseBlockId", path="src/")' }),
        S('res', { t: grepHits(12) }), S('res', { t: grepHits(8) }), S('res', { t: grepHits(11) }),
        S('tool', { t: 'grep(pattern="\\\\.slice\\\\(2[0-9]", path="src/")' }), S('tool', { t: 'grep(pattern="split\\\\(\\":\\"\\\\)", path="src/")' }),
        S('res', { t: grepHits(4) }), S('res', { t: grepHits(6) }),
        S('prose', { n: 2, g: ['report', 'caveat'] }), S('list', { n: 6, g: 'findings' })],
  },
  { // 6
    u: ['so while you were doing that I ran the nightly and got this\u2026 pasting the tail. this is the offsets thing I mentioned, it\u2019s been failing quietly for weeks I think',
      '[nightly #4471 tail]\nAssertionError: annotation span mismatch on session-047\n  expected span [1204, 1211) "thread\u00A0\uD83E\uDDF5"\n  received span [1203, 1210)\n  at verifySpans (annotate/check.ts:88)\n  at async run (annotate/nightly.ts:31)\nspan drift: +1 after first emoji, +2 after second\u2026 compounding\nupstream block: du:chat:wrk_9f31c2:block-v0:000041:00:000002'],
    r: [S('think', { w: 340 }), S('prose', { n: 3, g: ['explain', 'caveat', 'propose'] }),
        S('tool', { t: 'read_file(path="src/offsets.ts", offset=0, limit=80)' }), S('res', { t: excerpt(1, 30) })],
  },
  { // 7
    u: ['ok. before touching anything, write up the current architecture as you understand it. short. I\u2019ll forward it to the team so nobody is surprised next week.'],
    r: [S('think', { w: 200 }), S('prose', { n: 4, g: ['explain', 'report', 'propose', 'caveat'] }), S('list', { n: 8, g: 'findings' })],
  },
  { // 8 — single-line reply
    u: ['go ahead with the two-pass split idea'],
    r: [S('prose', { n: 1, g: ['ack'], pin: ['Starting with the extractor; emitBlocks\u2019 surface stays stable while pass one and pass two land underneath it.'] })],
  },
  { // 9 — the enormous thinking block
    u: ['one more consideration before you write the splitter — unicode. offsets today are utf-16 code units because javascript, but the annotation team counts graphemes, and at least one fixture has a \uD83E\uDDF5 emoji in a user message that pushes every downstream offset off by one\u2026 also windows line endings keep sneaking into fixtures via git on the intern\u2019s machine, so the CRLF thing is not hypothetical, it already burned us in march. think it through properly before proposing anything.'],
    r: [S('think', { w: 1400,
        open: ['This is the load-bearing question of the whole refactor, so it deserves the slow version. There are three separate unit systems in play and the code pretends there is one: UTF-16 code units (what String.length and slice count), Unicode code points (what the spread operator counts), and grapheme clusters (what a human — and the annotation team — counts). A thread emoji is one grapheme, one code point, two UTF-16 units. The moment one lands in a user message, every offset to its right disagrees by one depending on who is counting, and the disagreement compounds per emoji. Nothing crashes; the numbers are just quietly about different things.'],
        close: ['So the proposal I will actually make: pass one of the splitter records extents in UTF-16 units exactly as today — byte-identical output, no migration triggered. Alongside, and clearly marked experimental, each block also carries a grapheme extent computed via Intl.Segmenter, written to a sidecar field the wire format ignores. The annotation team can diff the sidecar against their own counts for a week of real traffic before anyone flips anything. CRLF gets one normalization at the single entry point, the fixture loader stops normalizing and starts rejecting, and session-047 becomes a named regression test. Nothing about this blocks the splitter work; it just has to land first so the splitter is built on normalized input from day one.'] }),
        S('prose', { n: 2, g: ['propose', 'caveat'] }), S('list', { n: 3, g: 'risks' })],
  },
  { // 10
    u: ['show me the diff for the ids.ts extraction first. just the diff, don\u2019t apply it yet.'],
    r: [S('tool', { t: 'edit_file(path="src/ids.ts", mode="preview", edits=4)' }), S('res', { t: DIFF_IDS }),
        S('prose', { n: 2, g: ['report', 'caveat'] })],
  },
  { // 11 — the all-caps turn
    u: ['NO. STOP. DO NOT RENAME THE EXPORTED SYMBOLS. makeBlockId and parseBlockId are imported by the annotation repo directly, I TOLD you the wire format is frozen and for us that includes the module surface\u2026 rename internals all you want but the exports stay EXACTLY as they are.',
      'also parseBlockId throwing on 6 segments is a behavior change — annotate passes short ids from before the part segment existed. tolerate 6, emit 7. add a test for it instead.'],
    r: [S('think', { w: 220 }), S('prose', { n: 2, g: ['ack', 'caveat'], pin: ['Understood — exported symbol names stay exactly as they are; ids.ts keeps makeBlockId and parseBlockId as the public surface, and parse goes back to tolerating six segments while make always emits seven.'] }),
        S('tool', { t: 'edit_file(path="src/ids.ts", mode="preview", edits=2)' }), S('res', { t: '@@ -14,7 +14,9 @@\n-  if (seg.length !== 7) throw new BadId(id);\n+  // tolerate 6 (pre-part ids from annotate); emit is always 7\n+  if (seg.length !== 6 && seg.length !== 7) throw new BadId(id);\n+  const part = seg.length === 7 ? +seg[5] : 0;' }),
        S('list', { n: 3, g: 'risks' })],
  },
  { // 12 — the huge tool result
    u: ['apply it and run the full suite. verbose. I want to see everything.'],
    r: [S('tool', { t: 'edit_file(path="src/ids.ts", apply=true)' }), S('res', { t: 'applied 4 edits across 4 files (src/ids.ts new, blockize.ts, emit/store.ts, dedupe.ts)' }),
        S('tool', { t: 'bash(cmd="pnpm vitest run --reporter=verbose")' }), S('res', { t: testLog(true) }),
        S('think', { w: 260 }), S('prose', { n: 2, g: ['report'] }), S('list', { n: 4, g: 'triage' })],
  },
  { // 13
    u: ['triage those failures — which are real regressions vs golden files being stale? do not regenerate anything yet.'],
    r: [S('think', { w: 300 }), S('tool', { t: 'read_file(path="fixtures/session-012.json", offset=0, limit=40)' }), S('res', { t: excerpt(1, 24) }),
        S('prose', { n: 2, g: ['report', 'caveat'] }), S('list', { n: 5, g: 'triage' })],
  },
  { // 14
    u: ['regen the goldens for the pure-format ones only. show me one diff in full before you commit anything.'],
    r: [S('tool', { t: 'bash(cmd="pnpm goldens:regen --filter format")' }), S('res', { t: REGEN_OUT }),
        S('tool', { t: 'bash(cmd="git diff fixtures/session-012.json")' }), S('res', { t: DIFF_NORM }),
        S('prose', { n: 2, g: ['report', 'caveat'] })],
  },
  { // 15 — one-liner both ways
    u: ['committed?'],
    r: [S('prose', { n: 1, g: ['ack'], pin: ['Committed as 9f31c2e — ids extraction plus the four format goldens, nothing else in the tree.'] })],
  },
  { // 16
    u: ['now the offsets bug from the nightly. walk the path end to end — from raw event to the span the annotation checker sees — and tell me exactly where the number goes wrong.'],
    r: [S('think', { w: 420 }), S('tool', { t: 'read_file(path="src/offsets.ts", offset=0, limit=120)' }), S('res', { t: excerpt(1, 38) }),
        S('tool', { t: 'read_file(path="src/normalize.ts")' }), S('res', { t: excerpt(1, 18) }),
        S('prose', { n: 3, g: ['explain', 'report', 'propose'] }), S('list', { n: 4, g: 'steps' })],
  },
  { // 17
    u: ['hm ok. so the immediate fix is in normalize, not the splitter\u2026 fine. do it, and run just the normalize + offsets specs.'],
    r: [S('tool', { t: 'edit_file(path="src/normalize.ts", apply=true, edits=2)' }), S('res', { t: DIFF_NORM }),
        S('tool', { t: 'bash(cmd="pnpm vitest run normalize offsets")' }), S('res', { t: testLog(false).split('\n').slice(0, 28).join('\n') }),
        S('prose', { n: 2, g: ['report', 'caveat'] })],
  },
  { // 18 — no tools, discussion
    u: ['before the splitter rewrite: convince me two passes won\u2019t be slower. this thing chews through every session we ingest, p95 matters more than elegance.',
      'and don\u2019t hand-wave with big-O, I want to know about allocations. the old splitter was rewritten off a naive version precisely because GC pauses were eating the p99.'],
    r: [S('think', { w: 380 }), S('prose', { n: 3, g: ['explain', 'propose', 'caveat'] }), S('list', { n: 5, g: 'risks' })],
  },
  { // 19
    u: ['benchmark it, don\u2019t guess.'],
    r: [S('tool', { t: 'bash(cmd="node bench/splitter.mjs --runs 200")' }), S('res', { t: BENCH_OUT }),
        S('prose', { n: 2, g: ['report'] }), S('list', { n: 3, g: 'risks' })],
  },
  { // 20
    u: ['good. next: the retry dedupe. what actually keys that window, and what happens on restart?'],
    r: [S('think', { w: 260 }), S('tool', { t: 'grep(pattern="tombstone|window.set", path="src/dedupe.ts")' }), S('res', { t: grepHits(8) }),
        S('tool', { t: 'read_file(path="src/dedupe.ts")' }), S('res', { t: excerpt(1, 42) }),
        S('prose', { n: 2, g: ['explain', 'report'] }), S('list', { n: 4, g: 'findings' })],
  },
  { // 21
    u: ['the last-write-wins thing bothers me\u2026 if upstream retries with different text we silently keep whichever came last?? that feels like data loss with extra steps. what would it take to at least SEE it happening — I don\u2019t want a behavior change mid-refactor, just visibility',
      'and to be clear the january tombstone incident means whatever you do, tombstones stay. we are not relitigating that.'],
    r: [S('think', { w: 340 }), S('prose', { n: 4, g: ['explain', 'propose', 'caveat', 'report'] })],
  },
  { // 22
    u: ['yes — hash beside the id, metric on mismatch, keep the tombstones. go.'],
    r: [S('prose', { n: 1, g: ['ack'], pin: ['On it — content hash beside the id in the retry window, a counter metric on mismatch, tombstones untouched.'] }),
        S('tool', { t: 'edit_file(path="src/dedupe.ts", apply=true, edits=3)' }), S('res', { t: DIFF_HASH })],
  },
  { // 23
    u: ['suite again. full.'],
    r: [S('tool', { t: 'bash(cmd="pnpm vitest run --reporter=verbose")' }), S('res', { t: testLog(false) }),
        S('prose', { n: 1, g: ['report'], pin: ['Green across all six files — 79 passed, the four regenerated goldens hold, and dedupe now reports text mismatches to metrics instead of swallowing them.'] })],
  },
  { // 24
    u: ['let\u2019s talk about the emitter before you touch it. what\u2019s the batching story today — and is the retry queue really re-sending whole batches? that would explain the duplicate spikes in the store dashboard.'],
    r: [S('think', { w: 300 }), S('tool', { t: 'read_file(path="src/emit/batch.ts")' }), S('res', { t: excerpt(1, 36) }),
        S('prose', { n: 3, g: ['explain', 'report', 'propose'] }), S('list', { n: 6, g: 'findings' })],
  },
  { // 25
    u: ['grep for every consumer of emitBlocks\u2019 batch flag\u2026 include the downstream jobs repo checkout in ../jobs if you can see it, they cargo-cult our config.'],
    r: [S('tool', { t: 'grep(pattern="batch", path="src/emit/")' }), S('res', { t: grepHits(9) }),
        S('tool', { t: 'grep(pattern="emitBlocks|batch:", path="../jobs/src/")' }), S('res', { t: grepHits(7) }),
        S('tool', { t: 'grep(pattern="EMIT_BATCH", path="../jobs/config/")' }), S('res', { t: grepHits(5) }),
        S('prose', { n: 2, g: ['report', 'caveat'] }), S('list', { n: 4, g: 'findings' })],
  },
  { // 26
    u: ['write the architecture note into docs/blockize.md. one page max, present tense, no aspirational stuff — describe what IS, mark what\u2019s frozen.'],
    r: [S('tool', { t: 'edit_file(path="docs/blockize.md", create=true)' }), S('res', { t: DOCS_OUT }),
        S('prose', { n: 1, g: ['report'], pin: ['The write-up is in docs/blockize.md — one page, present tense, with the frozen surfaces called out in the first section.'] })],
  },
  { // 27
    u: ['TODO sweep. grep TODO/FIXME/HACK in src, distill it — which of these does the refactor actually retire?'],
    r: [S('tool', { t: 'grep(pattern="TODO|FIXME|HACK", path="src/", include="*.ts")' }), S('res', { t: TODO_OUT }),
        S('prose', { n: 1, g: ['report'] }), S('list', { n: 8, g: 'todos' })],
  },
  { // 28 — no tools
    u: ['ok. stepping back — if we only land HALF of this before I get pulled onto the incident rotation, which half? prioritize for risk reduction, not for elegance.',
      'assume the intern can carry mechanical work forward but nothing judgment-heavy.'],
    r: [S('think', { w: 280 }), S('prose', { n: 3, g: ['propose', 'report', 'caveat'] }), S('list', { n: 5, g: 'steps' })],
  },
  { // 29
    u: ['ship the ids module extraction as PR 1 and the normalize fix as PR 2. splitter waits for me.'],
    r: [S('prose', { n: 2, g: ['ack', 'report'] }), S('tool', { t: 'bash(cmd="git push origin refactor/ids-module")' }), S('res', { t: GIT_OUT })],
  },
  { // 30 — page ends mid-work
    u: ['ok let\u2019s tackle the emitter next — same drill, read before write. start with store.ts and the retry queue.'],
    r: [S('think', { w: 240 }), S('tool', { t: 'read_file(path="src/emit/store.ts", offset=0, limit=160)' }), S('res', { t: excerpt(1, 52) })],
  },
];

// ---------- assembly ----------
const PROSE_ACK = ['Understood.', 'Done.', 'Running it now.'];
PROSE.ack = PROSE.ack || PROSE_ACK;

let EV = 0;
const turns = [];
const allBlocks = [];
const byId = {};
const turnOf = {};

function mkBlock(evNum, idx, type, actor, text) {
  const event = pad(evNum);
  const b = {
    id: 'du:chat:' + KEY + ':block-v0:' + event + ':00:' + pad(idx),
    sid: '\u2026:' + event + ':00:' + pad(idx),
    event, part: '00', idx, type, actor,
    text, words: wc(text), lines: text.split('\n').length,
  };
  allBlocks.push(b); byId[b.id] = b;
  return b;
}

SPEC.forEach((spec, ti) => {
  const n = ti + 1;
  const evFrom = EV;
  // user message + sub-chunks (same event; parent idx 0, chunks 1..N; 1-para msgs have no chunks)
  const paras = spec.u;
  const userEv = EV++;
  const parent = mkBlock(userEv, 0, 'user-message', 'user', paras.join('\n\n'));
  const chunks = paras.length > 1 ? paras.map((p, i) => mkBlock(userEv, i + 1, 'user-sub-chunk', 'user', p)) : [];
  // response
  const respEvFrom = EV;
  const response = [];
  let group = null; // { ev, count } for contiguous prose/list
  const flush = () => { group = null; };
  for (const seg of spec.r) {
    if (seg.k === 'prose' || seg.k === 'list') {
      if (!group) group = { ev: EV++, count: 0 };
      const texts = seg.k === 'prose'
        ? (seg.pin ? seg.pin.concat(proseBlocks(seg.n - seg.pin.length, seg.g)) : proseBlocks(seg.n, seg.g))
        : listBlocks(seg.n, seg.g);
      for (const t of texts.filter(Boolean)) {
        const type = seg.k === 'prose' ? 'agent-prose-paragraph' : 'agent-list-item';
        response.push(mkBlock(group.ev, group.count++, type, 'agent', t));
      }
    } else if (seg.k === 'think') {
      flush();
      response.push(mkBlock(EV++, 0, 'agent-thinking', 'agent', thinkText(seg.w, seg.open, seg.close)));
    } else if (seg.k === 'tool') {
      flush();
      response.push(mkBlock(EV++, 0, 'agent-tool-call', 'agent', seg.t));
    } else if (seg.k === 'res') {
      flush();
      response.push(mkBlock(EV++, 0, 'tool-result', 'tool', seg.t));
    }
  }
  const t = {
    n, n2: String(n).padStart(2, '0'),
    evFrom: pad(evFrom), evTo: pad(EV - 1),
    userEvent: pad(userEv),
    respEvFrom: pad(respEvFrom), respEvTo: pad(EV - 1),
    user: { block: parent, chunks },
    response,
    words: parent.words + response.reduce((a, b) => a + b.words, 0),
    blockCount: 1 + chunks.length + response.length,
  };
  turns.push(t);
  [parent, ...chunks, ...response].forEach((b) => { turnOf[b.id] = n; });
});

const totalWords = allBlocks.reduce((a, b) => a + b.words, 0);

export const meta = {
  key: KEY,
  keyFull: 'du:chat:' + KEY,
  title: 'blockize refactor \u2014 ingest pipeline',
  page: 1,
  pageComplete: false,
  turns: turns.length,
  events: EV,
  blocks: allBlocks.length,
  words: totalWords,
  nextEvent: pad(EV),
  moreHint: 'events ' + pad(EV) + '+ on page 2 \u2014 not loaded (page-complete? false)',
};
export { turns, allBlocks, byId, turnOf };
