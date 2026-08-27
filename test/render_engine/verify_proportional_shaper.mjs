import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const hb = await require("harfbuzzjs");

function bytes(relative) {
  const nodeBuffer = fs.readFileSync(path.join(repoRoot, relative));
  return nodeBuffer.buffer.slice(
    nodeBuffer.byteOffset,
    nodeBuffer.byteOffset + nodeBuffer.byteLength,
  );
}

function digest(relative) {
  return crypto.createHash("sha256").update(fs.readFileSync(path.join(repoRoot, relative))).digest("hex");
}

function openFont(relative) {
  const blob = hb.createBlob(bytes(relative));
  const face = hb.createFace(blob, 0);
  const font = hb.createFont(face);
  font.setScale(face.upem, face.upem);
  return { blob, face, font, unicodes: new Set(face.collectUnicodes()) };
}

function shape(font, text, { direction, features = "kern,liga,clig,calt" } = {}) {
  const buffer = hb.createBuffer();
  buffer.addText(text);
  buffer.setClusterLevel(1);
  if (direction) buffer.setDirection(direction);
  buffer.guessSegmentProperties();
  hb.shape(font, buffer, features);
  const glyphs = buffer.getGlyphInfosAndPositions();
  buffer.destroy();
  return glyphs;
}

function advance(glyphs) {
  return glyphs.reduce((sum, glyph) => sum + glyph.x_advance, 0);
}

function requireReceipt(condition, message, detail = {}) {
  if (!condition) {
    throw new Error(`${message}: ${JSON.stringify(detail)}`);
  }
}

const manifest = JSON.parse(fs.readFileSync(path.join(repoRoot, "resources/public/fonts/manifest.json"), "utf8"));
const config = manifest.fonts.find((font) => font.id === "ubuntu-sans-variable");
requireReceipt(config?.default === true, "proportional face is not the live default");

const primaryPath = `resources/public/fonts/${config.font}`;
const fallbackPath = `resources/public/fonts/${config.fallbacks[0].font}`;
const primary = openFont(primaryPath);
const fallback = openFont(fallbackPath);

primary.font.setVariations(config.variations);
const ligature = shape(primary.font, "office");
const ligatureClusters = [...new Set(ligature.map((glyph) => glyph.cluster))].sort((a, b) => a - b);
requireReceipt(ligature.length < "office".length, "ligature shaping did not reduce glyph count", { ligature });
requireReceipt(ligatureClusters.some((start, index) =>
  (ligatureClusters[index + 1] ?? "office".length) - start > 1),
"ligature did not preserve a multi-code-unit cluster", { ligatureClusters });

const kerned = advance(shape(primary.font, "AV", { features: "kern" }));
const unkerned = advance(shape(primary.font, "AV", { features: "kern=0" }));
requireReceipt(kerned !== unkerned, "kerning feature did not change the positioned advance", { kerned, unkerned });

const combining = shape(primary.font, "e\u0301");
requireReceipt(new Set(combining.map((glyph) => glyph.cluster)).size === 1,
  "combining sequence split across clusters", { combining });

const bidi = shape(primary.font, "سلام", { direction: "rtl" });
requireReceipt(bidi.some((glyph, index) => index > 0 && glyph.cluster < bidi[index - 1].cluster),
  "RTL shaping did not produce descending logical clusters", { bidi });

const fallbackCodepoint = 0x0250; // Latin small turned a: absent in Ubuntu Sans, present in Noto Sans.
requireReceipt(!primary.unicodes.has(fallbackCodepoint) && fallback.unicodes.has(fallbackCodepoint),
  "fallback corpus no longer crosses the declared face boundary", { fallbackCodepoint });
const fallbackGlyphs = shape(fallback.font, String.fromCodePoint(fallbackCodepoint));

const axes = primary.face.getAxisInfos();
requireReceipt(axes.wght && axes.wdth, "variable axes are absent", { axes });
primary.font.setVariations({ wght: 400, wdth: 75 });
const narrow = advance(shape(primary.font, "variable"));
primary.font.setVariations({ wght: 400, wdth: 125 });
const wide = advance(shape(primary.font, "variable"));
requireReceipt(narrow !== wide, "width axis did not alter positioned advances", { narrow, wide });
primary.font.setVariations(config.variations);

const slug = JSON.parse(fs.readFileSync(path.join(repoRoot, `resources/public/fonts/${config.slug.meta}`), "utf8"));
const slugGlyphs = new Set(slug.glyphs.map((glyph) => `${glyph.fontId}:${glyph.index}`));
const primaryGlyphIds = new Set([
  ...ligature.map((glyph) => glyph.codepoint),
  ...bidi.map((glyph) => glyph.codepoint),
  ...combining.map((glyph) => glyph.codepoint),
]);
requireReceipt([...primaryGlyphIds].every((glyphId) => slugGlyphs.has(`${config.id}:${glyphId}`)),
  "Slug metadata lacks a primary shaped glyph", { primaryGlyphIds: [...primaryGlyphIds] });
requireReceipt(fallbackGlyphs.every((glyph) => slugGlyphs.has(`${config.fallbacks[0].id}:${glyph.codepoint}`)),
  "Slug metadata lacks the fallback shaped glyph", { fallbackGlyphs });

const receipt = {
  contract: "T1-1/T1-2/proportional-shaper-assets",
  face: config.id,
  fallback: config.fallbacks[0].id,
  liveBackend: "slug",
  harfbuzz: hb.version_string(),
  fontDigests: { primary: digest(primaryPath), fallback: digest(fallbackPath) },
  corpus: {
    ligatureGlyphs: ligature.length,
    kerningDelta: unkerned - kerned,
    combiningClusters: new Set(combining.map((glyph) => glyph.cluster)).size,
    bidiClusters: bidi.map((glyph) => glyph.cluster),
    fallbackCodepoint: `U+${fallbackCodepoint.toString(16).toUpperCase()}`,
    variableWidthDelta: wide - narrow,
    explicitControls: ["tab", "newline"],
  },
  paintConsumers: {
    slugFaces: [...new Set(slug.glyphs.map((glyph) => glyph.fontId))],
  },
  pass: true,
};

console.log(`[T1-SHAPER] ${JSON.stringify(receipt)}`);
