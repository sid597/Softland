import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");

const owners = [
  ["src/app/client/substrate/connector_route.cljc", "layout-label", ["tl/layout"]],
  ["src/app/client/substrate/region3d_placement.cljc", "layout-placed-text", ["text-layout/layout"]],
  ["src/app/client/substrate/region3d_placement.cljc", "pack-glyph-quads", ["text-layout/paint-result"]],
  ["src/app/client/workspace/rect_tree.cljc", "wrap-line", ["tl/wrap-line"]],
  ["src/app/client/workspace/rect_tree.cljc", "resolve-text-layout", ["tl/layout", "tl/line-paint-ops"]],
  ["src/app/client/workspace/rect_tree.cljc", "tree->text-ops", ["tl/layout", "tl/clip-result"]],
  ["src/app/client/workspace/face_primitives.cljc", "text-run-prim", ["tl/layout", "tl/measure-result", "tl/line-paint-ops"]],
  ["src/app/client/workspace/face_primitives.cljc", "ground-block-layout", ["tl/layout"]],
  ["src/app/client/workspace/face_primitives.cljc", "block-render-lines", ["tl/layout", "tl/wrap-result"]],
  ["src/app/client/workspace/face_primitives.cljc", "block-root-prim", ["ground-block-layout", "tl/measure-result"]],
  ["src/app/client/workspace/face_primitives.cljc", "block-selection-wash-prim", ["tl/selection-result"]],
  ["src/app/client/workspace/face_primitives.cljc", "block-caret-prim", ["tl/caret-result"]],
  ["src/app/client/substrate/webgpu/renderer.cljs", "calculate-bracket-rects", ["tl/layout", "tl/selection-result"]],
  ["src/app/client/substrate/webgpu/renderer.cljs", "hit-test", ["tl/layout", "tl/hit-test-result"]],
  ["src/app/client/substrate/webgpu/renderer.cljs", "position-text-op", ["tl/layout", ":layout-result", ":glyphs", "line-index-for-layout"]],
  ["src/app/client/substrate/webgpu/renderer.cljs", "paint-msdf-line", ["painted-glyph"]],
  ["src/app/client/substrate/webgpu/renderer.cljs", "paint-slug-line", ["painted-glyph"]],
];

const paintConsumers = ["paint-msdf-line", "paint-slug-line"];
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
  if (relative.endsWith("text_layout_planes.cljc")) continue;
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
