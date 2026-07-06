# GATE REVIEW — Δ7 slug glyph-set expansion (t4-substrate)

**Stamp: Trunk-5 · 2026-07-06 · Fable gate (light — asset regen, zero product-code changes)**
**Verdict: PASS.** Assets committed-ready; switch-on remains Sid's call and must ride the 0.56→0.602 advance sweep (G17, face-2 wave).

Inputs: `BRANCH_REPORT_D7.md` + direct verification of every checkable claim
against the working tree (scripted, this session — not trusted from the report).

## Architecture

Pure asset regeneration through the in-repo generator
(`src-build/build/slug_font.clj`), glyph-list-driven from a regenerated
msdf-atlas-gen metrics JSON. No renderer change (verified: `renderer.cljs` not
in the wave diff); the glyph table, texture upload dims, and V3-5
tofu-with-advance fallback are all data-driven and absorb the wider set
untouched. `manifest.json` untouched — the app still renders the merged MSDF
interim; nothing switches on. Fence held exactly.

## Claims verified directly (script, this session)

| Claim | Result |
|---|---|
| 95 pre-D7 codepoints all present in new set | **VERIFIED** (0 missing) |
| Byte-identity of geometry on the 95 (`advance`/`planeBounds`/`sampleBounds`/`banding`/`bandMax`) | **VERIFIED — 0 diffs** (packing positions differ as disclosed; geometry does not) |
| 591 glyphs in slug meta + atlas.json; atlas 2048² | **VERIFIED** |
| Uniform advance across all 591 | **VERIFIED** — exactly one value, 0.60205078125 |
| Design set present (⊢ ⚑ ⚐ │ ├ └ • ✓ ✗ ● ◐ ○ ◌ ✎ ↔ ↓ ★ ⚠ ↻ ─ → �/U+FFFD) | **VERIFIED — all present** |
| ⟳ U+27F3 absent (font gap, disclosed) | **VERIFIED** (drawn-tofu-with-advance path applies) |
| Bin sizes match declared texture dims | **VERIFIED** — curve 163,840 B (4096×5 rgba16float), band 147,456 B (4096×9 rg16uint) |
| `.pre-d7.bak` hygiene | **CLEAN** — 5 backups on disk, all gitignored (`.gitignore:30 *.bak`); cannot ride a commit |
| `src-build/fonts/dejavu_slug_charset.txt` | present, 14 range entries (untracked — commits with the assets) |

## Failure modes attempted

- **Silent narrowing** (a pre-D7 glyph dropped or re-metriced): killed by the
  0-missing + 0-geometry-diff check above.
- **Advance drift inside the set** (a non-mono glyph sneaking in via
  box-drawing/symbol ranges): killed by the single-distinct-advance check.
- **Accidental switch-on** (manifest or renderer touched in the same wave):
  killed by git status/diff — neither file modified.
- **Backup leakage into the commit**: killed by check-ignore.

## Writers / readers / clearers

Assets are written only by the two-step regen (msdf-atlas-gen → generator);
read only by the font loader when the manifest selects `dejavu-sans-mono`
(currently it doesn't). No state lifecycle beyond files on disk.

## Async ordering / error path

None — no runtime code changed.

## Open doubts (carried, not blocking)

1. **The advance sweep is mandatory at switch-on** — ~30 hardcoded 0.56 sites
   vs the set's uniform 0.602051. Logged for G17 / the face-2 wave; flipping
   the manifest without the sweep drifts every cursor.
2. ⟳ U+27F3: accept tofu, or move the status-dot vocabulary to ↻ (in-set) —
   design-track call.
3. Astral/UTF-16 double-advance remains latent (sanitizer guards it today);
   becomes real only if emoji enter the design language.
4. Future cubic-outline (CFF/OTF) fonts are blocked on a quadratic-conversion
   step the generator lacks — the named wall stands.

## Gate ruling

Δ7 CLOSES as built. Commit scope: the 5 regenerated font assets +
`src-build/fonts/dejavu_slug_charset.txt` (code-tier commit, substrate
package). Switch-on is explicitly NOT part of this close.
