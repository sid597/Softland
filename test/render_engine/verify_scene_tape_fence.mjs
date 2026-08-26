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
const contracts = read("src/app/client/substrate/scene_tape.cljc");
const frameEffects = read("src/app/client/substrate/frame_effects.cljc");
const frameGraph = read("src/app/client/substrate/frame_graph.cljc");
const compositor = read("src/app/client/substrate/webgpu/compositor_gpu.cljs");

const drawFrame = findForm(renderer, "draw-frame!");
const executor = findForm(renderer, "execute-scene-tape!");
const entryExecutor = findForm(renderer, "execute-frame-entry!");
const twinCheck = findForm(renderer, "frame-tape-twin-check!");
const arrangementUpdate = findForm(renderer, "update-frame-arrangement");
const imageProducer = findForm(renderer, "image-entries");
const imageExecutor = findForm(renderer, "execute-image-batch!");
const imageResolver = findForm(renderer, "resolve-image-paint");
const registryStart = renderer.indexOf("(def frame-family-registry");
const registryEnd = renderer.indexOf("(def ^:private frame-contract-registry", registryStart);
if (registryStart < 0 || registryEnd < 0) {
  throw new Error("missing frame-family-registry slice");
}
const registry = renderer.slice(registryStart, registryEnd);
const registrationsStart = contracts.indexOf("(defn- registration");
const registrationsEnd = contracts.indexOf("(defn- require-keys!", registrationsStart);
if (registrationsStart < 0 || registrationsEnd < 0) {
  throw new Error("missing minimal family-registration slice");
}
const registrations = contracts.slice(registrationsStart, registrationsEnd);

const families = [
  ":render.family/msdf",
  ":render.family/clip",
  ":render.family/image",
  ":render.family/path",
  ":render.family/chrome",
  ":render.family/region-3d",
];
const effectDeclarations = [
  ":opacity",
  ":mask",
  ":layer-blur",
  ":backdrop-blur",
  ":isolate?",
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
requireToken("executor", executor, "execute-frame-entry!");
requireToken("entry executor", entryExecutor, "frame-family-registry");
requireToken("entry executor", entryExecutor, "(:execute! registration)");
forbid("executor", executor, /\(case\s+/, "family case dispatch instead of registration");
forbid("entry executor", entryExecutor, /\(case\s+/,
  "family case dispatch instead of registration");

// IMAGE-ATOM T10: the producer is a synchronous arrangement read. Decode,
// upload, promises, and resource construction stay in the ingress path.
// FRAME-RETENTION §5d moved sub-draw derivation from the producer to
// encode-time resolution; the pin moves with it (T7: pins move, never delete).
requireToken("image resolver", imageResolver, "contiguous-binding-runs");
requireToken("image producer", imageProducer, "(:stack-path source-order) 3");
forbid("image producer", imageProducer,
  /createImageBitmap|register-image-source!|copyExternalImageToTexture|writeTexture|\bPromise\b|\.then\s*\(|\bawait\b|\bfetch\b/,
  "decode/upload/async work entered the frame producer");
requireToken("image executor", imageExecutor, "sub-draws");
requireToken("image executor", imageExecutor, "first-instance");
// The registry was refactored into family/generic constructor helpers, so the
// literal ":execute! execute-image-batch!" token became structurally
// impossible. The pin MOVES (T7) to the same strength: the helper binds its
// executor argument under :execute!, and the image family passes
// execute-image-batch! as that argument.
requireToken("registry helper binds executor", registry, ":execute! execute!");
const imageExecutorBound =
  /:render\.family\/image\s+\(family\s[\s\S]{0,400}?execute-image-batch!\)/.test(registry);
if (!imageExecutorBound) {
  failures.push("image registry: execute-image-batch! is not the image family's :execute! argument");
}

requireToken("twin-check", twinCheck, "compile-frame-tape");
forbid("arrangement-update", arrangementUpdate, /frame-idx/,
  "execution frame counter entered maintained order derivation");

for (const family of families) {
  requireToken("family contracts", contracts, family);
  requireToken("frame executor registry", registry, family);
}

for (const [label, source, pattern] of [
  ["frame-plan orphan", frameGraph, /\(defn\s+maintain-frame-plan\b/],
  ["frame-plan state", frameGraph, /\(defn\s+empty-maintained-state\b/],
  ["effect-spans orphan", frameEffects, /\(defn\s+maintain-effect-spans\b/],
  ["effect-spans state", frameEffects, /\(defn\s+empty-maintained-state\b/],
  ["effect declarations helper", frameEffects, /\(defn-\s+declarations\b/],
  ["effect order-token helper", frameEffects, /\(defn-\s+arrangement-token\b/],
]) {
  forbid(label, source, pattern, "retired maintained twin closure returned");
}
for (const token of [
  ":family/version", ":grammar", ":provenance", ":versioning", ":receipts",
  ":edit-operations", ":serialization", ":migration",
]) {
  if (registrations.includes(token)) {
    failures.push(`family registration paper returned: ${token}`);
  }
}

for (const effect of effectDeclarations) {
  requireToken("effect grammar", frameEffects, effect);
}
for (const passKind of [":render", ":copy", ":present", ":readback", ":region"]) {
  requireToken("frame graph pass vocabulary", frameGraph, passKind);
}
requireToken("compositor", compositor, "draw-multipass!");
requireToken("compositor", compositor, "export-viewport!");
forbid("draw-frame effects", drawFrame,
  /\(case\s+[^)]*(?::opacity|:mask|:layer-blur|:backdrop-blur|:isolate\?)/s,
  "hand-positioned central effect branch");

requireToken("color seam", contracts, ":scene-color/linear-premultiplied-srgb");
requireToken("color seam", contracts, ":default-off? true");
requireToken("renderer default", renderer, "scene-color scene-tape/legacy-direct-color");

// IMAGE-ATOM T12: seed violations into the ACTUAL extracted production slice,
// so this self-test exercises both form extraction and the real forbids.
const centralSlice = `${drawFrame}\n${executor}`;
const seededCentralBranch = `${centralSlice}\n(case family :render.family/image (.draw pass 6))`;
const seededRejected = /\.draw\s+pass/.test(seededCentralBranch) &&
  /\(case\s+/.test(seededCentralBranch) &&
  !/\.draw\s+pass/.test(centralSlice) &&
  !/\(case\s+/.test(executor);
if (!seededRejected) failures.push("self-test: seeded family draw branch was not rejected");

const seededEffectBranch = `${drawFrame}\n(case effect-kind :effect/fake-family :draw-direct)`;
const seededEffectRejected = /\(case\s+[^)]*:effect\/fake-family/s.test(
  seededEffectBranch,
) && !/\(case\s+[^)]*:effect\/fake-family/s.test(drawFrame);
if (!seededEffectRejected) {
  failures.push("self-test: seeded central effect family was not rejected");
}

const receipt = {
  contract: "W2-B/admission+order+IMAGE/PATH/CHROME/REGION3D",
  families: families.length,
  registrationFields: 2,
  effectDeclarations: effectDeclarations.length,
  drawFrameBranches: 0,
  linearPremultipliedDefaultOff:
    contracts.includes(":scene-color/linear-premultiplied-srgb") &&
    contracts.includes(":default-off? true"),
  seededCentralBranchRejected: seededRejected,
  seededEffectFamilyRejected: seededEffectRejected,
  imageProducerPure:
    !/createImageBitmap|register-image-source!|copyExternalImageToTexture|writeTexture|\bPromise\b|\.then\s*\(|\bawait\b|\bfetch\b/.test(imageProducer),
  imageExecutorRegistered: imageExecutorBound,
  productionFailures: failures,
  pass: failures.length === 0,
};

console.log(`[W2-B-FENCE] ${JSON.stringify(receipt)}`);
if (!receipt.pass) process.exit(1);
