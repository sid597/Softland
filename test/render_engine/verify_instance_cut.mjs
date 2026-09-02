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
  "src/app/client/engine/device.cljs",
  "src/app/client/text/renderer.cljs",
  "src/app/client/image/renderer.cljs",
].map(read).join("\n");
absent(registrationSources, /render\.family\/connector|connector-registration/,
  "retired family is still registered");

const scene = read("src/app/client/region3d/scene.cljc");
const regionGpu = read("src/app/client/region3d/renderer.cljs");
absent(scene, /\b(?:gizmo-handles|translate-delta|rotate-delta|scale-ratio|maintain-scene)\b/,
  "retired Region3D interaction closure survived");
absent(regionGpu, /\b(?:grid-shader|overlay-glyph-shader|gizmo-shader|prepared-pick-state)\b/,
  "retired Region3D GPU closure survived");

const editAndPulseSources = [
  "src/app/client/path/component.cljc",
  "src/app/client/region3d/component.cljc",
  "src/app/client/engine/device.cljs",
  "src/app/client/text/renderer.cljs",
  "src/app/client/image/renderer.cljs",
].map(read).join("\n");
absent(editAndPulseSources,
  /\b(?:revisioned-edit|move-knot|set-knot-pressure|move-contour-point|replace-contours|edit-diff|apply-edit|pulse-alpha)\b/,
  "retired edit or pulse closure survived");

const sceneContractSources = [
  "src/app/client/region3d/scene.cljc",
  "src/app/client/engine/device.cljs",
  "src/app/client/text/renderer.cljs",
  "src/app/client/image/renderer.cljs",
  "src/app/client/path/renderer.cljs",
].map(read).join("\n");
absent(sceneContractSources,
  /\bpick-reverse\b|:region-router\b|:pick-order-derived\?|:resolve-view\b|:region-composite\b|:pass-class\s+:region\b/,
  "retired tape click/order vocabulary survived");
absent(sceneContractSources, /:pick\b/,
  "scene entries still carry per-entry pick data");

const rendererFiles = [
  "src/app/client/engine/device.cljs",
  "src/app/client/engine/compositor.cljs",
  "src/app/client/text/renderer.cljs",
  "src/app/client/image/renderer.cljs",
  "src/app/client/path/renderer.cljs",
  "src/app/client/region3d/renderer.cljs",
  "src/app/client/region3d/on_plane_renderer.cljs",
].map((relative) => path.join(root, relative));
const rendererFence = /\b(?:selection|marquee|gizmo|orbit|elbow|arrowhead|connector)\b/i;
for (const absolute of [...new Set(rendererFiles)]) {
  absent(fs.readFileSync(absolute, "utf8"), rendererFence,
    `${path.relative(root, absolute)} crossed the renderer fence`);
}

console.log(JSON.stringify({
  instanceCutTripwire: "pass",
  scenarios: 5,
  deleted: deleted.length,
  rendererFiles: new Set(rendererFiles).size,
}));
