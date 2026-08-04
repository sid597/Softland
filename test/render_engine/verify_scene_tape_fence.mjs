import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");

const read = (relative) =>
  fs.readFileSync(path.join(repoRoot, relative), "utf8");

function findForm(source, name) {
  const needles = [`(defn ${name}`, `(defn- ${name}`];
  const start = needles.map((needle) => source.indexOf(needle)).find((i) => i >= 0);
  if (start === undefined) throw new Error(`missing form ${name}`);

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
  throw new Error(`unterminated form ${name}`);
}

const renderer = read("src/app/client/substrate/webgpu/renderer.cljs");
const store = read("src/app/client/workspace/scene_store.cljc");
const contracts = read("src/app/client/substrate/scene_tape.cljc");
const runtime = read("src/app/client/workspace/runtime/render.cljs");

const drawFrame = findForm(renderer, "draw-frame!");
const executor = findForm(renderer, "execute-scene-tape!");
const twinCheck = findForm(renderer, "frame-tape-twin-check!");
const arrangementUpdate = findForm(renderer, "update-frame-arrangement");
const pick = findForm(store, "pick");
const registryStart = renderer.indexOf("(def ^:private frame-family-registry");
const registryEnd = renderer.indexOf("(defn- compile-frame-tape", registryStart);
const registry = renderer.slice(registryStart, registryEnd);

const families = [
  ":render.family/rect",
  ":render.family/shadow",
  ":render.family/msdf",
  ":render.family/slug",
  ":render.family/clip",
];

const failures = [];
// SEAM-STEP1 T7: ownership pins move to maintained names; no assertion or
// seeded self-test is deleted when the live path changes.
const requireToken = (label, source, token) => {
  if (!source.includes(token)) failures.push(`${label}: missing ${token}`);
};
const forbid = (label, source, pattern, reason) => {
  if (pattern.test(source)) failures.push(`${label}: ${reason}`);
};

requireToken("draw-frame", drawFrame, "frame-tape-twin-check!");
requireToken("draw-frame", drawFrame, "execute-scene-tape!");
forbid("draw-frame", drawFrame, /\.setPipeline\s+pass/, "hand-positioned pipeline branch");
forbid("draw-frame", drawFrame, /\.setBindGroup\s+pass/, "hand-positioned bind-group branch");
forbid("draw-frame", drawFrame, /\.setVertexBuffer\s+pass/, "hand-positioned vertex-buffer branch");
forbid("draw-frame", drawFrame, /\.draw\s+pass/, "hand-positioned draw branch");

requireToken("executor", executor, "scene-tape/paint-forward");
requireToken("executor", executor, "frame-family-registry");
requireToken("executor", executor, "(:execute! registration)");
forbid("executor", executor, /\(case\s+/, "family case dispatch instead of registration");

requireToken("twin-check", twinCheck, "compile-frame-tape");
forbid("arrangement-update", arrangementUpdate, /frame-idx/,
  "execution frame counter entered maintained order derivation");

requireToken("pick", pick, "scene-tape/pick-reverse");
requireToken("pick", pick, "containers/inverse-point");
requireToken("pick", pick, "maintained-entries");
forbid("pick", pick, /sort-by/, "independent pick sort");
forbid("pick", pick, /#\(-\s*\(:layer/, "legacy layer-descending truth");

for (const family of families) {
  requireToken("family contracts", contracts, family);
  requireToken("frame executor registry", registry, family);
}

requireToken("color seam", contracts, ":scene-color/linear-premultiplied-srgb");
requireToken("color seam", contracts, ":default-off? true");
requireToken("runtime target", runtime, "use-persistent-render-target? false");
requireToken("camera sink", runtime, "@ground/!camera");
forbid("camera derivation", runtime, /\(m\/watch\s+ground\/!camera\)/,
  "ground camera watch remains inside scene derivation");
requireToken("renderer default", renderer, ":or {scene-color-enabled? false}");

const seededCentralBranch = `(defn draw-frame! [pass family]
  (case family :rect (.draw pass 6)))`;
const seededRejected = /\.draw\s+pass/.test(seededCentralBranch) &&
  /\(case\s+/.test(seededCentralBranch);
if (!seededRejected) failures.push("self-test: seeded family draw branch was not rejected");

const receipt = {
  contract: "W2-B/O-G-M-C",
  families: families.length,
  drawFrameBranches: 0,
  reversePick: pick.includes("scene-tape/pick-reverse"),
  cameraSinkQuarantined:
    runtime.includes("@ground/!camera") &&
    !/\(m\/watch\s+ground\/!camera\)/.test(runtime),
  linearPremultipliedDefaultOff:
    contracts.includes(":scene-color/linear-premultiplied-srgb") &&
    contracts.includes(":default-off? true"),
  seededCentralBranchRejected: seededRejected,
  productionFailures: failures,
  pass: failures.length === 0,
};

console.log(`[W2-B-FENCE] ${JSON.stringify(receipt)}`);
if (!receipt.pass) process.exit(1);
