import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");

const owners = [
  ["src/app/client/workspace/rect_tree.cljc", "wrap-line", ["tl/wrap-line"]],
  ["src/app/client/workspace/rect_tree.cljc", "resolve-text-layout", ["tl/layout", "tl/line-paint-ops"]],
  ["src/app/client/workspace/rect_tree.cljc", "tree->text-ops", ["tl/layout", "tl/clip-result"]],
  ["src/app/client/workspace/face_primitives.cljc", "text-run-prim", ["tl/layout", "tl/measure-result", "tl/line-paint-ops"]],
  ["src/app/client/workspace/face_primitives.cljc", "ground-block-layout", ["tl/layout"]],
  ["src/app/client/workspace/face_primitives.cljc", "block-render-lines", ["tl/layout", "tl/wrap-result"]],
  ["src/app/client/workspace/face_primitives.cljc", "block-root-prim", ["ground-block-layout", "tl/measure-result"]],
  ["src/app/client/workspace/face_primitives.cljc", "block-selection-wash-prim", ["tl/selection-result"]],
  ["src/app/client/workspace/face_primitives.cljc", "block-caret-prim", ["tl/caret-result"]],
  ["src/app/client/workspace/combined_text.cljs", "clip-editor-text-op", ["tl/layout", "tl/clip-result"]],
  ["src/app/client/workspace/runtime/mouse.cljs", "handle-cmd-click!", ["tl/layout", "tl/hit-test-result"]],
  ["src/app/client/workspace/runtime/mouse.cljs", "handle-editor-click!", ["tl/layout", "tl/hit-test-result"]],
  ["src/app/client/substrate/webgpu/renderer.cljs", "calculate-bracket-rects", ["tl/layout", "tl/selection-result"]],
  ["src/app/client/substrate/webgpu/renderer.cljs", "hit-test", ["tl/layout", "tl/hit-test-result"]],
  ["src/app/client/substrate/webgpu/renderer.cljs", "shape-msdf-line", ["tl/layout", "tl/paint-result"]],
  ["src/app/client/substrate/webgpu/renderer.cljs", "shape-slug-line", ["tl/layout", "tl/paint-result"]],
];

const privateMetricPatterns = [
  ["round/ceil/floor", /Math\/(?:round|ceil|floor)/],
  ["divide by private metric", /\(\s*\/[^\n)]{0,120}\b(?:char-w|char-advance|line-h)\b/],
  ["count-times-metric", /\(\s*\*[^\n)]{0,120}\((?:count|tl\/code-unit-count)\b/],
  ["metric-times-count", /\(\s*\*[^\n)]{0,120}\b(?:char-w|char-advance)\b[^\n)]{0,80}\((?:count|tl\/code-unit-count)\b/],
  ["private substring clip", /\bsubs\b/],
  ["private line split", /\bstr\/split-lines\b/],
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
for (const [relative, name, required] of owners) {
  const source = files.get(relative) ?? fs.readFileSync(path.join(repoRoot, relative), "utf8");
  files.set(relative, source);
  const body = findOwner(source, name);
  for (const failure of auditOwner(body, required)) {
    failures.push(`${relative}#${name}: ${failure}`);
  }
}

const seededPrivateConsumer = `(defn seeded-private-consumer [text char-advance]
  (* (count text) char-advance))`;
const seedFailures = auditOwner(seededPrivateConsumer, ["tl/layout"]);
if (!seedFailures.includes("count-times-metric")) {
  failures.push("self-test: seeded count-times-metric consumer was not rejected");
}

const receipt = {
  contract: "T0-3/no-independent-metrics",
  owners: owners.length,
  files: files.size,
  seededPrivateConsumerRejected: seedFailures.length > 0,
  productionFailures: failures,
  pass: failures.length === 0,
};

console.log(`[T0-FENCE] ${JSON.stringify(receipt)}`);
if (!receipt.pass) process.exit(1);
