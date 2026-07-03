# WebGPU Editor - Usage Documentation

## Font Rendering Settings

The editor uses **MSDF (Multi-channel Signed Distance Field)** rendering for resolution-independent text. Two key parameters control text appearance:

---

## pxRange (Pixel Range)

**What it is:** The "spread" of the distance field in the font atlas, measured in pixels.

When generating the MSDF atlas with `msdf-atlas-gen -pxrange 8`:
- Each pixel stores how far it is from the glyph edge
- `pxRange 8` means the distance field extends 8 pixels around each glyph edge
- Higher = smoother scaling, but uses more atlas space
- Lower = sharper but can get jagged at large sizes

**In the shader:**
```glsl
let screenPxRange = params.pxRange * (visual_size / params.atlasEmSize);
```
This scales the pxRange based on how big you're rendering the font vs. the atlas size.

---

## Sharpness

**What it is:** An offset to the distance threshold that determines where the glyph edge is drawn.

**In the shader:**
```glsl
let dist = sd - 0.5 + params.sharpness;
let opacity = clamp(dist * screenPxRange + 0.5, 0.0, 1.0);
```

| Sharpness | Effect | Visual |
|-----------|--------|--------|
| **-0.2** | Threshold moves inward | Thinner strokes, crisper edges |
| **0** | Standard MSDF | Normal rendering |
| **+0.2** | Threshold moves outward | Thicker strokes, softer/blurrier |

### Why Terminal Text Looks Different

Terminal fonts use **bitmap hinting** - pixels are either ON or OFF, aligned to pixel grid.

MSDF uses **smooth gradients** - the `opacity` calculation produces values between 0-1, creating anti-aliased edges.

**Negative sharpness** makes the transition zone smaller, approaching that binary ON/OFF look of terminals.

---

## Visual Explanation

```
pxRange = how wide the "fuzzy zone" is around glyph edges
sharpness = where within that zone we draw the edge

         pxRange spread
    |<------8px------>|

    ░░░▒▒▓██████▓▒▒░░░   <- Distance field gradient
          ^
          |
    sharpness controls where this threshold is

    sharpness = -0.1: edge drawn HERE (thinner)
    sharpness =  0.0: edge drawn here (normal)
    sharpness = +0.1: edge drawn here (thicker)
```

---

## Recommended Settings

For **terminal-like crispness**, try:
- **Sharpness:** `-0.1` to `-0.15`
- **pxRange:** `8` (default)
- **Line Height:** `1.0`

---

## Keyboard Shortcuts

### Settings Panel (Ctrl+G)
| Key | Action |
|-----|--------|
| `Tab` | Switch between Fonts and Sliders panes |
| `Up/Down` | Navigate items |
| `Left/Right` | Adjust slider values |
| `Enter/Escape` | Close settings |

### Editor
| Key | Action |
|-----|--------|
| `Ctrl+G` | Open settings panel |
| `Ctrl+E` | Evaluate form at cursor |
| `Ctrl+Z` | Undo |
| `Ctrl+Shift+Z` | Redo |

---

## Font Atlas Generation

To add new fonts, use `msdf-atlas-gen`:

```bash
msdf-atlas-gen -font "/path/to/Font.ttf" \
  -type msdf \
  -size 64 \
  -pxrange 8 \
  -pots \
  -format png \
  -imageout font_atlas.png \
  -json font_atlas.json
```

Then add the font to `resources/public/fonts/manifest.json`.
