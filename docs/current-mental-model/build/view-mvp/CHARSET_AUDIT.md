# Character-Set Coverage Audit — WebGPU Text Renderer

*Generated 2026-07-04. Scope: the font-atlas glyph tables vs. the material the trail view will render (all `docs/**` + `vision/**` markdown, and the Claude transcript `*.jsonl` under `~/.claude/projects/-mnt-data-projects-Softland/`).*

The renderer can only draw a codepoint if it has a glyph in the loaded font atlas; every other codepoint is silently dropped. This audit measures how much of the real material falls into that gap.

---

## Headline

- **Every font atlas is printable-ASCII-only.** All 7 atlas/meta JSONs (`font_atlas.json`, its `.bak`, all four `fonts/*_atlas.json`, and the DejaVu slug meta) carry the **exact same 95 glyphs: U+0020–U+007E**. Identical codepoint sets (same md5). Nothing outside ASCII exists in any atlas — no matter which font the manifest selects, coverage is the same 95 glyphs.
- **Unique-codepoint coverage of the material: 10.3% combined (95/922), 28.4% for the authored markdown (95/334).** The material uses ~922 distinct codepoints; the atlas draws 95 of them.
- **Volume coverage looks high but is flattered by ASCII plumbing: 99.56% combined, 96.62% for markdown.** The missing 0.4%/3.4% is not random — it is concentrated in exactly the structural glyphs this project writes in: box-drawing (trees & tables), arrows (the `Q→C→E→D→R→F` / `Projection → ActionRequest` notation), em/en dashes, curly quotes, bullets, check/cross marks.
- **Breakage is widespread, not marginal:** **69.8%** of markdown documents and **19.4%** of non-blank markdown lines contain at least one un-renderable glyph; **26.8%** of transcript records (messages) do too. A dropped `→` inside `Projection → ActionRequest` silently changes meaning; a dropped `─│├└` shatters every ASCII-art tree and table into rubble.
- **The single worst character is `─` (U+2500 box-drawing horizontal): 1.13 million occurrences** across the material — the renderer cannot draw a single one of them today.

---

## 1. Atlases found

Under `resources/public/` (`resources/public/` root + `resources/public/fonts/`):

| Atlas / meta JSON | Glyphs | Codepoint range | Covers beyond ASCII? |
|---|---:|---|---|
| `font_atlas.json (top-level / active)` | 95 | U+0020–U+007E | No |
| `font_atlas.json.bak` | 95 | U+0020–U+007E | No |
| `fonts/ubuntu_sans_mono_atlas.json` | 95 | U+0020–U+007E | No |
| `fonts/ubuntu_mono_atlas.json` | 95 | U+0020–U+007E | No |
| `fonts/dejavu_sans_mono_atlas.json` | 95 | U+0020–U+007E | No |
| `fonts/noto_sans_mono_atlas.json` | 95 | U+0020–U+007E | No |
| `fonts/dejavu_sans_mono_slug_meta.json (slug)` | 95 | U+0020–U+007E | No |

**All 7 files are identical in coverage** (`all_identical = true`; same md5 of the sorted codepoint set = `9ac8f9df`). Each covers **exactly the 95 printable ASCII codepoints U+0020…U+007E** — no C1 controls, no Latin-1 supplement, no punctuation, no symbols, no CJK, no emoji.

Per `resources/public/fonts/manifest.json`, **DejaVu Sans Mono** is the default (`"default": true`, `preferredBackend: "slug"`); Ubuntu Sans/Mono and Noto are marked `available: false`. The top-level `font_atlas.json` is the active MSDF atlas. **Font choice does not matter for coverage** — all of them, MSDF and slug, ship the same ASCII-only glyph set. The `.png`/`.bin` payloads differ; the *character repertoire* does not.

---

## 2. Material corpus

- **Markdown:** 356 files at histogram time (docs corpus is live — 358 at final recount; a 2-file drift during the audit, immaterial to the percentages). 14,951,292 characters. Includes `vision/LOG.md` and everything under `docs/**` (which contains `docs/vision/**`).
- **Transcripts:** 1292 `*.jsonl` files, 578,756,874 characters (~579 MB).
- **Combined:** 593,708,166 characters, 922 distinct codepoints.

**Codepoint counting** is by real Unicode codepoint (Python native), so astral characters (emoji) count as one codepoint each.

**JSON-escape noise — measured, and it is negligible.** The task flagged that some transcript characters could appear as literal `\uXXXX` sequences. Empirically they do not: in a 150-file sample, **26.8% of records contain raw non-ASCII bytes** but only **0.2% contain any `\u` escape**. Claude Code stores message text as raw UTF-8, so the histogram sees real codepoints, not escapes. (Literal `\n`/`\t`-style backslash sequences do appear inside quoted code samples, but those are legitimately ASCII content and do not hide non-ASCII glyphs.) The high transcript volume-coverage number is caused by ASCII *plumbing* — JSON keys, UUIDs, timestamps, tool payloads, code — diluting the non-ASCII fraction, **not** by escaping.

**Two measurement artifacts** (called out for honesty, both negligible): U+FFFD REPLACEMENT CHARACTER appears 1,634 times — these are this audit's own `errors='replace'` decode substitutions, not material. And U+0000 NUL appears ~6,376 times (null padding in a few transcripts); per the safety rule it is only ever shown escaped as `<U+0000>` in this report.

---

## 3. Coverage summary (per atlas — all identical, so one set of numbers)

| Corpus | Unique codepoints | Unique covered | **Unique %** | Total volume | Volume covered | **Volume %** |
|---|---:|---:|---:|---:|---:|---:|
| Combined (md + transcripts) | 922 | 95 | **10.30%** | 593,708,166 | 591,100,362 | **99.56%** |
| Markdown only (authored) | 334 | 95 | **28.44%** | 14,951,292 | 14,445,410 | **96.62%** |
| Transcripts only | 918 | 95 | **10.35%** | 578,756,874 | 576,654,952 | **99.64%** |

**Reading these numbers.** Volume % is the optimistic lens — most bytes in any corpus are ASCII letters, digits, and punctuation the atlas *does* cover. Unique % is the honest lens on the material's *alphabet*: the renderer can draw 95 of the ~922 distinct codepoints in use (~10%). The markdown-only row is the cleanest authored signal (no transcript plumbing): **28.4% of its alphabet, and 96.6% of its volume** — meaning ~3.4% of every authored character stream is un-drawable, and that 3.4% is the load-bearing structure (arrows, box-drawing, dashes).

---

## 4. How badly does real material break?

Volume % hides the damage because breakage is *clustered* — one diagram packs hundreds of box glyphs onto a few lines. The document- and line-level view:

| Breakage lens | Rate |
|---|---:|
| Markdown **documents** with ≥1 un-renderable glyph | **250 / 358 = 69.8%** |
| Non-blank markdown **lines** with ≥1 un-renderable glyph | **36,934 / 190,422 = 19.4%** |
| Transcript **records** (messages) with ≥1 un-renderable glyph (150-file sample) | **2,472 / 9,230 = 26.8%** |

*("un-renderable glyph" here excludes non-drawing codepoints — newline, tab, NUL, no-break space, decode-replacement, private-use — so this counts only characters the renderer would actually try to draw and fail.)*

Roughly **seven in ten** authored documents and **one in five** authored lines would render with at least one silent hole. The failure is not cosmetic: the project's core notation (`Q→C→E→D→R→F`, `source → view`, the center-loop arrows) and its entire vocabulary of trees, tables, and status glyphs live in the missing set.

---

## 5. Top 40 missing codepoints — combined material, by frequency

827 distinct codepoints in the material have no atlas glyph. The 40 most frequent:

| U+XXXX | char | name | count | example context (±7 chars) |
|---|---|---|---:|---|
| U+2500 | `─` | BOX DRAWINGS LIGHT HORIZONTAL | 1,133,533 |       ┌──────── |
| U+2192 | `→` | RIGHTWARDS ARROW | 371,437 |  parse → extrac |
| U+000A ⓝ | n/a | <unnamed, category Cc> | 332,161 | 0d1e5"}<U+000A>{"type" |
| U+2014 | `—` | EM DASH | 219,483 | g mode — I'll s |
| U+2550 | `═` | BOX DRAWINGS DOUBLE HORIZONTAL | 165,667 |     ;; ════════ |
| U+2502 | `│` | BOX DRAWINGS LIGHT VERTICAL | 108,420 |        │        |
| U+2501 | `━` | BOX DRAWINGS HEAVY HORIZONTAL | 23,173 | 48\t   ━━━━━━━━ |
| U+2551 | `║` | BOX DRAWINGS DOUBLE VERTICAL | 22,983 | ╗\n68\t║        |
| U+00B7 | `·` | MIDDLE DOT | 18,283 | tokens · /autoc |
| U+00B6 | `¶` | PILCROW SIGN | 17,400 |  msg-2 ¶2)   │\ |
| U+2013 | `–` | EN DASH | 12,544 |  now, 2–4 optio |
| U+2591 | `░` | LIGHT SHADE | 11,270 | 15\t\|  ░░░░░░░░ |
| U+00A7 | `§` | SECTION SIGN | 9,515 | ec.md\` §1.4 sto |
| U+2019 | `’` | RIGHT SINGLE QUOTATION MARK | 8,661 | of code’s exist |
| U+2026 | `…` | HORIZONTAL ELLIPSIS | 7,851 | rt from…","sess |
| U+00D7 | `×` | MULTIPLICATION SIGN | 7,243 |  10–100× — a re |
| U+251C | `├` | BOX DRAWINGS LIGHT VERTICAL AND RIGHT | 7,118 | gestor ├──> Obj |
| U+2514 | `└` | BOX DRAWINGS LIGHT UP AND RIGHT | 6,962 | │\n    └─────── |
| U+0000 ⓝ | n/a | <unnamed, category Cc> | 6,376 | boot"}<U+000A><U+0000><U+0000><U+0000><U+0000><U+0000><U+0000><U+0000><U+0000> |
| U+201C | `“` | LEFT DOUBLE QUOTATION MARK | 5,466 | e name “code in |
| U+201D | `”` | RIGHT DOUBLE QUOTATION MARK | 5,455 | ngestor”: code  |
| U+253C | `┼` | BOX DRAWINGS LIGHT VERTICAL AND HORIZONTAL | 4,982 | d ─────┼──► WOR |
| U+2588 | `█` | FULL BLOCK | 4,940 | UDE    ████████ |
| U+2510 | `┐` | BOX DRAWINGS LIGHT DOWN AND LEFT | 4,737 | ───────┐\n      |
| U+25BC | `▼` | BLACK DOWN-POINTING TRIANGLE | 4,586 |        ▼        |
| U+250C | `┌` | BOX DRAWINGS LIGHT DOWN AND RIGHT | 4,531 |        ┌─────── |
| U+2518 | `┘` | BOX DRAWINGS LIGHT UP AND LEFT | 4,513 | ───────┘  └──── |
| U+2022 | `•` | BULLET | 3,087 | se:\n  • Identi |
| U+252C | `┬` | BOX DRAWINGS LIGHT DOWN AND HORIZONTAL | 2,805 | ───────┬─────── |
| U+2524 | `┤` | BOX DRAWINGS LIGHT VERTICAL AND LEFT | 2,797 | ───────┤\n807\t |
| U+2190 | `←` | LEFTWARDS ARROW | 2,675 |        ← gates  |
| U+203A | `›` | SINGLE RIGHT-POINTING ANGLE QUOTATION MARK | 2,398 | -panel*› \n╰─➤  |
| U+00A0 ⓝ | n/a | NO-BREAK SPACE | 2,378 | tent":"<U+00A0>Here I  |
| U+2713 | `✓` | CHECK MARK | 2,307 | es ? \"✓\" : \" |
| U+2039 | `‹` | SINGLE LEFT-POINTING ANGLE QUOTATION MARK | 2,298 | tland  ‹agent-o |
| U+2605 | `★` | BLACK STAR | 2,238 | o.\n\n\`★ Insigh |
| U+2194 | `↔` | LEFT RIGHT ARROW | 2,040 | n rows ↔ git co |
| U+FFFD ⓝ | `�` | REPLACEMENT CHARACTER | 1,634 | (tofu/\`�\`), sur |
| U+2264 | `≤` | LESS-THAN OR EQUAL TO | 1,493 | commit ≤ 14:03  |
| U+25CF | `●` | BLACK CIRCLE | 1,410 | \| \"\\n● \\(.cl |

ⓝ = **non-glyph codepoint**: a control/format/whitespace/artifact character (newline U+000A, tab U+0009, NUL U+0000, no-break space U+00A0, decode-replacement U+FFFD, private-use). These are "missing from the atlas" in the literal sense but are not glyphs the renderer draws — newlines/tabs are layout controls, NUL is data padding, U+FFFD is this audit's decode artifact. They are shown for completeness and excluded from the breakage counts in §4. **Every other row is a visible glyph the renderer silently drops.** All control characters are rendered in escaped `<U+XXXX>` form in the context column, never as raw bytes.

---

## 6. Top missing codepoints — markdown corpus only (authored signal)

Stripping transcript plumbing isolates what *authored docs* rely on. The 30 most frequent missing codepoints in `docs/**` + `vision/**`:

| U+XXXX | char | name | count | example context (±7 chars) |
|---|---|---|---:|---|
| U+000A ⓝ | n/a | <unnamed, category Cc> | 240,233 | 0d1e5"}<U+000A>{"type" |
| U+2500 | `─` | BOX DRAWINGS LIGHT HORIZONTAL | 170,938 |       ┌──────── |
| U+2502 | `│` | BOX DRAWINGS LIGHT VERTICAL | 18,995 |        │        |
| U+2550 | `═` | BOX DRAWINGS DOUBLE HORIZONTAL | 17,164 |     ;; ════════ |
| U+2014 | `—` | EM DASH | 13,476 | g mode — I'll s |
| U+2192 | `→` | RIGHTWARDS ARROW | 6,184 |  parse → extrac |
| U+00B7 | `·` | MIDDLE DOT | 5,300 | tokens · /autoc |
| U+2551 | `║` | BOX DRAWINGS DOUBLE VERTICAL | 3,929 | ╗\n68\t║        |
| U+2501 | `━` | BOX DRAWINGS HEAVY HORIZONTAL | 2,898 | 48\t   ━━━━━━━━ |
| U+2019 | `’` | RIGHT SINGLE QUOTATION MARK | 2,185 | of code’s exist |
| U+2591 | `░` | LIGHT SHADE | 1,983 | 15\t\|  ░░░░░░░░ |
| U+201C | `“` | LEFT DOUBLE QUOTATION MARK | 1,356 | e name “code in |
| U+201D | `”` | RIGHT DOUBLE QUOTATION MARK | 1,353 | ngestor”: code  |
| U+253C | `┼` | BOX DRAWINGS LIGHT VERTICAL AND HORIZONTAL | 1,252 | d ─────┼──► WOR |
| U+2013 | `–` | EN DASH | 1,164 |  now, 2–4 optio |
| U+00A7 | `§` | SECTION SIGN | 1,147 | ec.md\` §1.4 sto |
| U+2588 | `█` | FULL BLOCK | 1,076 | UDE    ████████ |
| U+251C | `├` | BOX DRAWINGS LIGHT VERTICAL AND RIGHT | 991 | gestor ├──> Obj |
| U+2514 | `└` | BOX DRAWINGS LIGHT UP AND RIGHT | 966 | │\n    └─────── |
| U+2026 | `…` | HORIZONTAL ELLIPSIS | 894 | rt from…","sess |
| U+0009 ⓝ | n/a | <unnamed, category Cc> | 810 | <U+000A>     1<U+0009>0 * * * |
| U+2510 | `┐` | BOX DRAWINGS LIGHT DOWN AND LEFT | 808 | ───────┐\n      |
| U+250C | `┌` | BOX DRAWINGS LIGHT DOWN AND RIGHT | 777 |        ┌─────── |
| U+2518 | `┘` | BOX DRAWINGS LIGHT UP AND LEFT | 770 | ───────┘  └──── |
| U+2524 | `┤` | BOX DRAWINGS LIGHT VERTICAL AND LEFT | 675 | ───────┤\n807\t |
| U+00D7 | `×` | MULTIPLICATION SIGN | 620 |  10–100× — a re |
| U+00A0 ⓝ | n/a | NO-BREAK SPACE | 615 | tent":"<U+00A0>Here I  |
| U+25BC | `▼` | BLACK DOWN-POINTING TRIANGLE | 592 |        ▼        |
| U+252C | `┬` | BOX DRAWINGS LIGHT DOWN AND HORIZONTAL | 459 | ───────┬─────── |
| U+2022 | `•` | BULLET | 303 | se:\n  • Identi |

Box-drawing dominates: the authored docs are full of ASCII-art trees, tables, and diagrams. Em-dash, curly quotes, middot (`·`, used in status dots and inline separators), and the `→` arrow round out the top of the authored list.

---

## 7. Specific-character checks (requested set)

Every requested character is **absent from the atlas** (as expected — the atlas is ASCII-only). Counts show how often each appears in the material.

| U+XXXX | char | name | in atlas? | count (combined) | count (md) | count (jsonl) | example context |
|---|---|---|---|---:|---:|---:|---|
| U+00B7 | `·` | MIDDLE DOT | **no** | 18,283 | 5,300 | 12,983 | tokens · /autoc |
| U+2013 | `–` | EN DASH | **no** | 12,544 | 1,164 | 11,380 |  now, 2–4 optio |
| U+2014 | `—` | EM DASH | **no** | 219,483 | 13,476 | 206,007 | g mode — I'll s |
| U+2018 | `‘` | LEFT SINGLE QUOTATION MARK | **no** | 325 | 1 | 324 | he old ‘continu |
| U+2019 | `’` | RIGHT SINGLE QUOTATION MARK | **no** | 8,661 | 2,185 | 6,476 | of code’s exist |
| U+201C | `“` | LEFT DOUBLE QUOTATION MARK | **no** | 5,466 | 1,356 | 4,110 | e name “code in |
| U+201D | `”` | RIGHT DOUBLE QUOTATION MARK | **no** | 5,455 | 1,353 | 4,102 | ngestor”: code  |
| U+2022 | `•` | BULLET | **no** | 3,087 | 303 | 2,784 | se:\n  • Identi |
| U+2026 | `…` | HORIZONTAL ELLIPSIS | **no** | 7,851 | 894 | 6,957 | rt from…","sess |
| U+2190 | `←` | LEFTWARDS ARROW | **no** | 2,675 | 258 | 2,417 |        ← gates  |
| U+2191 | `↑` | UPWARDS ARROW | **no** | 209 | 17 | 192 | ws → ← ↑ ↓, bul |
| U+2192 | `→` | RIGHTWARDS ARROW | **no** | 371,437 | 6,184 | 365,253 |  parse → extrac |
| U+2193 | `↓` | DOWNWARDS ARROW | **no** | 622 | 26 | 596 |  → ← ↑ ↓, bulle |
| U+2500 | `─` | BOX DRAWINGS LIGHT HORIZONTAL | **no** | 1,133,533 | 170,938 | 962,595 |       ┌──────── |
| U+2502 | `│` | BOX DRAWINGS LIGHT VERTICAL | **no** | 108,420 | 18,995 | 89,425 |        │        |
| U+2514 | `└` | BOX DRAWINGS LIGHT UP AND RIGHT | **no** | 6,962 | 966 | 5,996 | │\n    └─────── |
| U+251C | `├` | BOX DRAWINGS LIGHT VERTICAL AND RIGHT | **no** | 7,118 | 991 | 6,127 | gestor ├──> Obj |
| U+2605 | `★` | BLACK STAR | **no** | 2,238 | 172 | 2,066 | o.\n\n\`★ Insigh |
| U+26A0 | `⚠` | WARNING SIGN | **no** | 267 | 24 | 243 | note\">⚠ {H.esc |
| U+2705 | `✅` | WHITE HEAVY CHECK MARK | **no** | 1,390 | 157 | 1,233 | oji (⚠ ✅ ❌ 🔴 🟢  |
| U+2713 | `✓` | CHECK MARK | **no** | 2,307 | 261 | 2,046 | es ? \"✓\" : \" |
| U+2717 | `✗` | BALLOT X | **no** | 927 | 97 | 830 | \" : \"✗\"))\n2 |
| U+274C | `❌` | CROSS MARK | **no** | 429 | 25 | 404 | (str \"❌ \" err |
| U+27F3 | `⟳` | CLOCKWISE GAPPED CIRCLE ARROW | **no** | 55 | 5 | 50 |  ❌ 🔴 🟢 ⟳), and  |
| U+1F534 | `🔴` | LARGE RED CIRCLE | **no** | 11 | 2 | 9 | (⚠ ✅ ❌ 🔴 🟢 ⟳),  |
| U+1F7E2 | `🟢` | LARGE GREEN CIRCLE | **no** | 8 | 0 | 8 |  ✅ ❌ 🔴 🟢 ⟳), an |

Highlights: curly quotes `’ ‘ “ ”` (U+2018–201D) total ~19.9k occurrences; em/en dash `— –` ~232k; ellipsis `…` 7.9k; the arrow family `→ ← ↑ ↓` ~375k (dominated by `→`); box-drawing `─ │ ├ └` >1.25M; bullet `•`/middot `·` ~21.4k; check/cross `✓ ✗ ✅ ❌` ~4.1k; the `★` star 2.2k. Emoji are rare but present: `⚠` 267, `⟳` 55, `🔴` 11, `🟢` 8 — each a single codepoint, each undrawable.

---

## 8. Method & caveats

- **Codepoint iteration** is Python-native, so astral characters (emoji beyond the BMP) count as one codepoint each, not as surrogate pairs.
- **Atlas glyph sets** were read from the `glyphs[].unicode` field of each JSON (the slug meta uses the same field). All 7 hashed to the same 95-codepoint set.
- **Files were decoded UTF-8 with `errors='replace'`**, injecting 1,634 U+FFFD replacement characters for undecodable bytes — a negligible audit artifact, flagged in §2.
- **JSON-escape noise** was measured (§2) and is negligible (0.2% of records) — transcripts store non-ASCII raw.
- **Control-byte safety:** no raw control byte (NUL/U+0000 or any C0/C1) is written anywhere in this report. Context snippets escape every control character to `<U+XXXX>` form, and `|`/`` ` `` are escaped for table integrity. A final assertion in the generator re-scans the whole document to guarantee this.
- **Corpus is live:** the markdown set moved 356→358 during the audit (a parallel session). Percentages are unaffected at this precision.
- **All atlases identical**, so the per-atlas coverage numbers in §3 and the missing tables in §5–§7 apply equally to every font the renderer can load.

