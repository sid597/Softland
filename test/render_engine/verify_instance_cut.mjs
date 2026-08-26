#!/usr/bin/env node

import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, "../..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const absent = (source, pattern, label) =>
  assert.doesNotMatch(source, pattern, label);

const deleted = [
  "src/app/client/substrate/connector_material.cljc",
  "src/app/client/substrate/connector_route.cljc",
  "src/app/client/substrate/webgpu/connector_gpu.cljs",
  "src/app/client/workspace/region3d_pointer.cljc",
  "src/app/client/substrate/frame_scheduler.cljc",
];
for (const relative of deleted) {
  assert.equal(fs.existsSync(path.join(root, relative)), false, `${relative} survived`);
}

const registrationSources = [
  "src/app/client/substrate/scene_tape.cljc",
  "src/app/client/substrate/frame_inputs.cljc",
  "src/app/client/substrate/webgpu/renderer.cljs",
].map(read).join("\n");
absent(registrationSources, /render\.family\/connector|connector-registration/,
  "retired family is still registered");

const neutralSources = [
  "src/app/client/substrate/chrome_material.cljc",
  "src/app/client/substrate/scene_tape.cljc",
  "src/app/client/substrate/frame_inputs.cljc",
  "src/app/client/substrate/webgpu/renderer.cljs",
].map(read).join("\n");
for (const form of [":selection-outline", ":handle", ":marquee", ":guide-line", ":gap-tick"]) {
  assert.equal(neutralSources.includes(form), false, `${form} survived neutralization`);
}
absent(neutralSources, /:chrome\/selection\b/, "mark input still carries identity");

const scene = read("src/app/client/substrate/region3d_scene.cljc");
const regionGpu = read("src/app/client/substrate/webgpu/region3d_gpu.cljs");
absent(scene, /\b(?:gizmo-handles|translate-delta|rotate-delta|scale-ratio|maintain-scene)\b/,
  "retired Region3D interaction closure survived");
absent(regionGpu, /\b(?:grid-shader|overlay-glyph-shader|gizmo-shader|prepared-pick-state)\b/,
  "retired Region3D GPU closure survived");

const editAndPulseSources = [
  "src/app/client/substrate/path_material.cljc",
  "src/app/client/substrate/region3d_material.cljc",
  "src/app/client/substrate/webgpu/chrome_gpu.cljs",
  "src/app/client/substrate/webgpu/renderer.cljs",
].map(read).join("\n");
absent(editAndPulseSources,
  /\b(?:revisioned-edit|move-knot|set-knot-pressure|move-contour-point|replace-contours|edit-diff|apply-edit|pulse-alpha)\b/,
  "retired edit or pulse closure survived");

const sceneContractSources = [
  "src/app/client/substrate/scene_tape.cljc",
  "src/app/client/substrate/frame_graph.cljc",
  "src/app/client/substrate/region3d_scene.cljc",
  "src/app/client/substrate/webgpu/renderer.cljs",
  "src/app/client/substrate/webgpu/path_gpu.cljs",
  "src/app/client/substrate/webgpu/chrome_gpu.cljs",
].map(read).join("\n");
absent(sceneContractSources,
  /\bpick-reverse\b|:region-router\b|:pick-order-derived\?|:resolve-view\b|:region-composite\b|:pass-class\s+:region\b/,
  "retired tape click/order vocabulary survived");
absent(sceneContractSources, /:pick\b/,
  "scene entries still carry per-entry pick data");

const gpuDir = path.join(root, "src/app/client/substrate/webgpu");
const painterFiles = fs.readdirSync(gpuDir)
  .filter((name) => name.endsWith("_gpu.cljs"))
  .map((name) => path.join(gpuDir, name));
painterFiles.push(path.join(gpuDir, "renderer.cljs"), path.join(gpuDir, "compositor_gpu.cljs"));
const painterFence = /\b(?:selection|marquee|gizmo|orbit|elbow|arrowhead|connector)\b/i;
for (const absolute of [...new Set(painterFiles)]) {
  absent(fs.readFileSync(absolute, "utf8"), painterFence,
    `${path.relative(root, absolute)} crossed the painter fence`);
}

console.log(JSON.stringify({
  instanceCutTripwire: "pass",
  scenarios: 5,
  deleted: deleted.length,
  painterFiles: new Set(painterFiles).size,
}));
