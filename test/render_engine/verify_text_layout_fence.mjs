import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");

const owners = [
  ["src/app/client/region3d/on_plane.cljc", "layout-placed-text", ["text-layout/layout"]],
  ["src/app/client/text/renderer.cljs", "position-text-draw-item", ["tl/layout", ":layout-result", "tl/glyph-indexes-in-source-range", "line-index-for-layout"]],
  // The flat paint route: planes reach the instance words only through the
  // pack entry point (SHAPER-BORDER.md §6.3/§6.4); the packer never owns layout.
  ["src/app/client/text/glyph_pack.cljs", "each-painted!", ["tl/pack-glyphs!"]],
  ["src/app/client/text/glyph_pack.cljs", "pack-draw-item!", ["each-painted!"]],
];

const paintConsumers = ["each-painted!", "pack-draw-item!"];
const forbiddenPaintOwnership = ["tl/layout", "tl/legacy-char-advance", "tl/measure-result", "tl/hit-test-result"];

const privateMetricPatterns = [
  ["round/ceil/floor", /Math\/(?:round|ceil|floor)/],
  ["divide by private metric", /\(\s*\/[^\n)]{0,120}\b(?:char-w|char-advance|line-h)\b/],
  ["count-times-metric", /\(\s*\*[^\n)]{0,120}\((?:count|tl\/code-unit-count)\b/],
  ["metric-times-count", /\(\s*\*[^\n)]{0,120}\b(?:char-w|char-advance)\b[^\n)]{0,80}\((?:count|tl\/code-unit-count)\b/],
  ["private substring clip", /\bsubs\b/],
  ["private line split", /\bstr\/split-lines\b/],
];

const rawPlaneTokens = [
  ":glyph-id+flags",
  ":position-x",
  ":span-source-start",
  ":span-glyph-start",
];

function findOwner(source, name) {
  const needles = [`(defn ${name}`, `(defn- ${name}`];
  const start = needles.map((needle) => source.indexOf(needle)).find((i) => i >= 0);
  if (start === undefined) throw new Error(`missing owner ${name}`);

  let depth = 0;
  let inString = false;
  let escaped = false;
  let inComment = false;
  for (let i = start; i < source.length; i += 1) {
    const ch = source[i];
    if (inComment) {
      if (ch === "\n") inComment = false;
      continue;
    }
    if (inString) {
      if (escaped) escaped = false;
      else if (ch === "\\") escaped = true;
      else if (ch === '"') inString = false;
      continue;
    }
    if (ch === ";") inComment = true;
    else if (ch === '"') inString = true;
    else if (ch === "(") depth += 1;
    else if (ch === ")") {
      depth -= 1;
      if (depth === 0) return source.slice(start, i + 1);
    }
  }
  throw new Error(`unterminated owner ${name}`);
}

function auditOwner(body, required) {
  const failures = [];
  for (const token of required) {
    if (!body.includes(token)) failures.push(`missing delegation ${token}`);
  }
  for (const [label, pattern] of privateMetricPatterns) {
    if (pattern.test(body)) failures.push(label);
  }
  return failures;
}

const files = new Map();
const failures = [];
// SEAM-STEP1 T7: the adopted pins move with the stable/overlay split while
// every existing ownership assertion and seeded negative remains alive.
for (const [relative, name, required] of owners) {
  const source = files.get(relative) ?? fs.readFileSync(path.join(repoRoot, relative), "utf8");
  files.set(relative, source);
  const body = findOwner(source, name);
  for (const failure of auditOwner(body, required)) {
    failures.push(`${relative}#${name}: ${failure}`);
  }
  if (paintConsumers.includes(name)) {
    for (const token of forbiddenPaintOwnership) {
      if (body.includes(token)) failures.push(`${relative}#${name}: paint owns ${token}`);
    }
  }
}

// Layout-retention: raw column ownership ends at the accessor namespace.
// Scan every production CLJ/CLJS source so a new consumer cannot evade the
// seeded owner list merely by appearing in a new namespace.
const sourceRoot = path.join(repoRoot, "src/app");
const sourceFiles = [];
const walkSources = (dir) => {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const absolute = path.join(dir, entry.name);
    if (entry.isDirectory()) walkSources(absolute);
    else if (/\.clj[cs]$/.test(entry.name)) sourceFiles.push(absolute);
  }
};
walkSources(sourceRoot);
for (const absolute of sourceFiles) {
  const relative = path.relative(repoRoot, absolute);
  if (relative === "src/app/client/text/layout_planes.cljc") continue;
  const source = fs.readFileSync(absolute, "utf8");
  for (const token of rawPlaneTokens) {
    if (source.includes(token)) failures.push(`${relative}: raw plane read ${token}`);
  }
}

const seededPrivateConsumer = `(defn seeded-private-consumer [text char-advance]
  (* (count text) char-advance))`;
const seedFailures = auditOwner(seededPrivateConsumer, ["tl/layout"]);
if (!seedFailures.includes("count-times-metric")) {
  failures.push("self-test: seeded count-times-metric consumer was not rejected");
}

const seededRawPlaneConsumer = `(:position-x (:layout/planes layout-result))`;
const seededRawPlaneRejected = rawPlaneTokens.some((token) =>
  seededRawPlaneConsumer.includes(token),
);
if (!seededRawPlaneRejected) {
  failures.push("self-test: seeded raw-plane consumer was not rejected");
}

const receipt = {
  contract: "T1/no-independent-metrics-or-backend-placement",
  owners: owners.length,
  files: files.size,
  seededPrivateConsumerRejected: seedFailures.length > 0,
  seededRawPlaneConsumerRejected: seededRawPlaneRejected,
  paintConsumers,
  productionFailures: failures,
  pass: failures.length === 0,
};

console.log(`[T1-FENCE] ${JSON.stringify(receipt)}`);
if (!receipt.pass) process.exit(1);
