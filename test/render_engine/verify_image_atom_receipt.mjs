import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

const root = process.cwd();
const artifactArg = process.argv[2];
if (!artifactArg) {
  throw new Error("usage: node verify_image_atom_receipt.mjs <phase-artifact>");
}

const read = (file) => fs.readFileSync(path.join(root, file), "utf8");
const json = (file) => JSON.parse(read(file));
const sha256 = (file) =>
  crypto.createHash("sha256").update(fs.readFileSync(path.join(root, file))).digest("hex");
const assert = (condition, label, details = undefined) => {
  if (!condition) {
    throw new Error(`IMAGE-ATOM receipt assertion failed: ${label}${
      details === undefined ? "" : ` ${JSON.stringify(details)}`
    }`);
  }
};

const artifact = read(artifactArg);
const match = artifact.match(
  /<!-- IMAGE_ATOM_RECEIPT_JSON\n([\s\S]*?)\nIMAGE_ATOM_RECEIPT_JSON -->/,
);
assert(match, "machine-receipt-block");
const phase = JSON.parse(match[1]);

const statusLines = execFileSync(
  "git",
  ["status", "--porcelain=v1", "-uall"],
  { cwd: root, encoding: "utf8" },
)
  .trimEnd()
  .split("\n")
  .filter(Boolean);
const changedFiles = statusLines
  .map((line) => {
    const raw = line.slice(3);
    return raw.includes(" -> ") ? raw.split(" -> ").at(-1) : raw;
  })
  .sort();

const allowedExact = new Set([
  "src/app/client/substrate/scene_tape.cljc",
  "src/app/client/substrate/webgpu/renderer.cljs",
  "src/app/client/workspace/scene_store.cljc",
  "src/app/client/workspace/rect_tree.cljc",
  "src/app/client/workspace/scene_runtime.cljs",
  "src/app/client/substrate/webgpu/gpu_budget.cljs",
  "src/app/client/substrate/webgpu/verifier.cljs",
  "test/render_engine/verify_scene_tape_fence.mjs",
  "test/render_engine/run_verifier.mjs",
  "test/app/fixtures/render_engine/gpu-goldens/manifest.json",
  "test/app/fixtures/render_engine/gpu-goldens/environment.json",
  "test/app/test_runner.clj",
  "src/app/client/substrate/image_material.cljc",
  "test/render_engine/verify_image_atom_receipt.mjs",
  "test/app/client/substrate/image_material_test.clj",
  "test/app/client/substrate/image_citizenship_test.clj",
  "docs/current-mental-model/build/render-engine/IMAGE-ATOM-P1.md",
  "docs/current-mental-model/build/render-engine/IMAGE-ATOM-NOW.md",
]);
const allowed = (file) =>
  allowedExact.has(file) ||
  /^test\/app\/fixtures\/render_engine\/gpu-goldens\/gpu-image-atom-[a-z0-9-]+\.png$/.test(
    file,
  ) ||
  /^test\/app\/fixtures\/render_engine\/images\/[a-z0-9_-]+\.(png|clj)$/.test(
    file,
  );

assert(
  JSON.stringify(changedFiles) === JSON.stringify([...phase.changedFiles].sort()),
  "diff-derived-changed-file-sum",
  { actual: changedFiles, artifact: phase.changedFiles },
);
assert(changedFiles.every(allowed), "section-12-allowlist", {
  refused: changedFiles.filter((file) => !allowed(file)),
});
assert(
  changedFiles.filter(
    (file) =>
      file.startsWith("test/app/fixtures/render_engine/gpu-goldens/") &&
      file.endsWith(".png") &&
      !path.basename(file).startsWith("gpu-image-atom-"),
  ).length === 0,
  "legacy-golden-pngs-untouched",
);

assert(phase.schemaVersion === 1, "phase-schema-version");
assert(phase.status === "STOP-S2", "exact-stop-s2");
assert(phase.opening.branch === "docs/current-mental-model-local", "opening-branch");
assert(
  phase.opening.head === "eb0ef7b4e284d689f4e396a249372fdba3260bad",
  "opening-head",
);
assert(phase.opening.clean === true, "opening-clean");
assert(
  sha256("docs/current-mental-model/build/render-engine/IMAGE-ATOM-CONTRACT.md") ===
    "ebd894a58af945b761907decad74f8e7f3d39ea0a744ccadc6ce168900783800",
  "freeze-anchor",
);
assert(phase.opening.freezeAnchorMatched === true, "opening-freeze-attestation");
assert(
  phase.attestation.isFallbackAdapter === true &&
    phase.attestation.renderer === "swiftshader" &&
    phase.attestation.environmentFingerprint ===
      "5ced2482f3d0b8e9a14465bba06808a04d3a5a9b7d386fcf71f40a30f557343e",
  "attestation-first-fields",
);
assert(
  artifact.indexOf("## Attestation") >= 0 &&
    artifact.indexOf("## Attestation") < artifact.indexOf("## Outcome"),
  "attestation-first-order",
);

const receipt = json("target/render-verifier/receipt.json");
const stdout = receipt.stdoutSummary;
assert(receipt.classification === "candidate-pick-parity-failure", "red-classification");
assert(receipt.sourceMatch === true && receipt.environmentMatch === true, "source-environment-match");
assert(receipt.assertion.requested === true && receipt.assertion.pass === true, "assert-mode-green");
assert(stdout.existingGoldens === "21/21", "legacy-golden-floor");
assert(stdout.deterministic === "21/21", "legacy-determinism-floor");
assert(stdout.candidateParity === "14/21", "preserved-msdf-counterexample");
assert(stdout.productBoundsDivergenceSentinels === "7/7", "legacy-sentinels");
assert(stdout.imageGoldens === "21/21", "image-golden-floor");
assert(stdout.imageDeterminism === "21/21", "image-determinism-floor");
assert(stdout.imageParity === "14/14", "image-parity-floor");
assert(receipt.imageAtom.pass === true, "image-contract-browser-receipt");
assert(
  receipt.imageAtom.pickParity.rows.length === 14 &&
    receipt.imageAtom.pickParity.rows.every(
      (row) => row.pass && row.boundaryPixelCount > 0 && row.decisiveCount > 0,
    ),
  "image-pick-numeric-floors",
);
assert(receipt.imageAtom.goldenComparison.rows.length === 21, "image-golden-row-count");
assert(receipt.imageAtom.determinism.rows.length === 21, "image-determinism-row-count");
assert(receipt.imageAtom.color.pass === true, "image-color-receipt");
assert(receipt.imageAtom.arrangement.pass === true, "image-arrangement-receipt");
assert(receipt.imageAtom.lifecycle.pass === true, "image-lifecycle-receipt");
assert(receipt.imageAtom.productLoopClaim === false, "no-product-loop-claim");
assert(receipt.imageAtom.productLoopJoin === "staged", "product-loop-staged");
assert(receipt.imageAtom.darkWave === true && receipt.imageAtom.feltGate === false, "dark-wave");

const fixtureDigests = Object.fromEntries(
  Object.keys(phase.fixtureDigests).map((filename) => [
    filename,
    sha256(`test/app/fixtures/render_engine/images/${filename}`),
  ]),
);
assert(
  JSON.stringify(fixtureDigests) === JSON.stringify(phase.fixtureDigests),
  "fixture-digests",
  { actual: fixtureDigests, artifact: phase.fixtureDigests },
);

const locatorGroups = {
  "src/app/client/substrate/scene_tape.cljc": [
    "(def family-ids",
    "(def scene-color-seam",
    "(defn- regime",
    "(defn- registration",
    "(def family-contracts",
    "required-regime-keys",
    "validate-regimes!",
    "validate-family!",
    "(defn register-family",
    "(defn compare-scalar",
    "(defn compare-order",
    "(defn compile-tape",
    "(defn paint-forward",
    "(defn pick-reverse",
  ],
  "src/app/client/substrate/webgpu/renderer.cljs": [
    "(def max-transform-nodes",
    "(defn create-containers-buffer",
    "(defn init-rect-system",
    "(defn init-image-system",
    "(defn create-camera-buffer",
    "(defn init-text-system",
    "(defn init-shadow-system",
    "create-msdf-font-resources",
    "create-slug-texture",
    "(defn- frame-order",
    "(defn- gpu-paint",
    "store-pool-entries",
    "execute-gpu-batch!",
    "(def frame-family-registry",
    "frame-contract-registry",
    "!frame-arrangement",
    "(defn produce-frame-entries",
    "(defn update-frame-arrangement",
    "(defn compile-frame-tape",
    "frame-tape-twin-check!",
    "(defn draw-frame!",
    "(defn init-clear-quad",
  ],
  "src/app/client/workspace/scene_store.cljc": [
    "(defn- flatten-ops",
    "(defn- stamp-ops-container",
    "(defn upsert-slot",
    "(defn- deepest-addressed",
    "(defn- slot-entry",
    "(defn scene-tape",
    "(defn rebuild-ordered",
    "(defn maintained-entries",
    "(defn pick",
  ],
  "src/app/client/workspace/rect_tree.cljc": [
    "(defn rt-node",
    "(defn tree->rects",
    "(defn tree->text-ops",
    "(defn tree->shadows",
    "(defn hit-test",
  ],
  "src/app/client/workspace/scene_runtime.cljs": ["(defn <store-frame"],
  "src/app/client/workspace/containers.cljc": [
    "(defn effective",
    "(defn inverse-point",
  ],
  "src/app/client/substrate/webgpu/buffer_pool.cljs": [
    "(defn create-pool",
    "(defn pool-draw-info",
  ],
  "src/app/client/substrate/webgpu/gpu_budget.cljs": [
    "(defn register-texture!",
    "(defn replace-texture!",
  ],
  "src/app/client/substrate/webgpu/verifier.cljs": [
    "zoom-cases",
    "render-system-bytes!",
    "run-case!",
  ],
  "test/render_engine/verify_scene_tape_fence.mjs": ["const families ="],
  "test/render_engine/run_verifier.mjs": [
    "serveSyntheticOrigin",
    "productionInputs",
    "appendAuthorized",
    "imageComparison",
    "classification",
    "stdoutSummary",
  ],
  "test/app/test_runner.clj": ["(defn- test-inventory", "(defn full"],
  "src/app/client/workspace/runtime/fonts.cljs": ["createImageBitmap"],
  "package.json": ["verify:text-layout", "verify:render-engine"],
};
const missingLocators = [];
let locatorCount = 0;
for (const [file, locators] of Object.entries(locatorGroups)) {
  const source = read(file);
  for (const locator of locators) {
    locatorCount += 1;
    if (!source.includes(locator)) missingLocators.push(`${file}:${locator}`);
  }
}
assert(missingLocators.length === 0, "section-11-locator-substance", missingLocators);
assert(phase.locators.status === "all-substance-present", "locator-artifact-status");
assert(phase.locators.reverifiedCount === locatorCount, "locator-count", {
  actual: locatorCount,
  artifact: phase.locators.reverifiedCount,
});
const bankManifest = json("test/app/fixtures/render_engine/gpu-goldens/manifest.json");
assert(bankManifest.images.length === 21, "section-11-legacy-bank-rows");
assert(bankManifest.imageAtomCases.length === 21, "section-11-image-bank-rows");
assert(
  fs.existsSync(
    path.join(root, "test/app/fixtures/render_engine/gpu-goldens/environment.json"),
  ),
  "section-11-environment-bank",
);
for (const file of [
  "docs/current-mental-model/build/render-engine/W1.md",
  "docs/current-mental-model/build/render-engine/ENGINE.md",
  "docs/current-mental-model/build/render-engine/W0-C.md",
  "docs/current-mental-model/build/render-engine/W2-B-T1.md",
  "test/render_engine/verify_shaping_correction_fence.mjs",
]) {
  assert(fs.existsSync(path.join(root, file)), `section-11-context:${file}`);
}

assert(phase.gates.jvmFull.exit === 1, "jvm-full-red");
assert(phase.gates.jvmFull.failures === 5 && phase.gates.jvmFull.errors === 0, "jvm-full-counts");
assert(phase.gates.jvmFull.packageNamespacesPassed === true, "package-jvm-assertions-green");
assert(
  phase.gates.jvmFull.blockingPath === "src/app/client/workspace/ground.cljs" &&
    !changedFiles.includes(phase.gates.jvmFull.blockingPath),
  "s2-protected-blocking-path-untouched",
);
assert(
  phase.falsifier.findings.length === 3 &&
    phase.falsifier.findings.every((finding) => finding.disposition === "corrected-and-receipted"),
  "bounded-falsifier-findings",
);
assert(phase.imageAboveTextKindLayerDefault === true, "image-above-text-default");

console.log(
  `[IMAGE-ATOM-PHASE] ${JSON.stringify({
    status: phase.status,
    changedFiles: changedFiles.length,
    locators: locatorCount,
    imageGoldens: stdout.imageGoldens,
    imageParity: stdout.imageParity,
    renderAssertion: receipt.assertion.pass,
    jvmFull: `${phase.gates.jvmFull.failures} failures/${phase.gates.jvmFull.errors} errors`,
    stop: phase.stop.clause,
    pass: true,
  })}`,
);
