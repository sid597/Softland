#!/usr/bin/env node

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
const layout = read("src/app/client/workspace/text_layout.cljc");
const ground = read("src/app/client/workspace/ground.cljs");
const primitives = read("src/app/client/workspace/face_primitives.cljc");
const runtime = read("src/app/client/workspace/runtime/render.cljs");
const profileHarness = read("test/render_engine/profile_shaping_correction.mjs");

const positionTextOp = findForm(renderer, "position-text-op");
const clipResult = findForm(layout, "clip-result");
const glyphSelection = findForm(layout, "glyphs-in-source-range");
const spanSelection = findForm(layout, "spans-for-source-range");
const carryGround = findForm(ground, "carry-ground-text-layouts!");
const acquireGround = findForm(ground, "acquire-ground-layout!");
const seedStale = findForm(ground, "seed-stale-layout!");
const anatomyView = findForm(ground, "block-anatomy-view-model");
const installGround = findForm(ground, "install-ground!");
const foldState = findForm(ground, "set-fold-state!");
const profileTargets = findForm(ground, "profile-targets");
const profileIdentity = findForm(ground, "profile-identity");
const blockLayout = findForm(primitives, "ground-block-layout");
const resetCacheCounters = findForm(layout, "layout-cache-reset-counters");
const reconcileGeos = findForm(runtime, "reconcile-slot-text-geos!");
const paintCause = findForm(runtime, "slot-paint-cause");

const failures = [];
const requireToken = (label, body, token) => {
  if (!body.includes(token)) failures.push(`${label}: missing ${token}`);
};
const forbid = (label, body, pattern, reason) => {
  if (pattern.test(body)) failures.push(`${label}: ${reason}`);
};

requireToken("renderer paint selection", positionTextOp, "tl/glyphs-in-source-range");
requireToken("renderer line selection", positionTextOp, "tl/line-by-id");
forbid("renderer paint selection", positionTextOp,
  /\(filter[\s\S]{0,500}\(:glyphs\s+line\)/,
  "full line-glyph filter returned");

requireToken("clip selection", clipResult, "glyphs-in-source-range");
requireToken("clip line selection", clipResult, "line-by-id");
forbid("clip selection", clipResult,
  /\(filter[\s\S]{0,500}\(:clusters\s+line\)/,
  "full line-cluster filter returned");

requireToken("shared glyph selection", glyphSelection,
  "glyph-indexes-for-source-range");
requireToken("span seek", spanSelection, "first-owned-span-index");
forbid("span seek", spanSelection, /\(filter\s/,
  "span reader linearly filters every retained span");

requireToken("ground carry", carryGround, ":layout-result");
requireToken("ground carry", carryGround, "acquire-ground-layout!");
requireToken("ground cache prune", carryGround, "layout-cache-remove-addresses");
requireToken("material-local derivation", anatomyView, ":layout-result layout-result");
requireToken("primitive carried result", blockLayout, "carried-result");
requireToken("primitive carried result", blockLayout, "carried-result carried-result");
requireToken("counter-only cache reset", resetCacheCounters,
  "assoc (or cache (empty-layout-cache))");
requireToken("counter-only cache reset", resetCacheCounters, ":hits 0");
requireToken("dynamic paint cause", paintCause, "some :paint/cause");
requireToken("oracle hit cause stability", acquireGround, ":layout/cause cached-result");
requireToken("oracle-only stale seed", seedStale, ":receipts :output-hash");
forbid("oracle-only stale seed", seedStale, /assoc\s+:layout\/id/,
  "negative hook changes renderer-visible layout identity");
requireToken("runtime identity", reconcileGeos, "slot-layout-token");
requireToken("runtime paint partition", reconcileGeos, "slot-paint-token");
requireToken("runtime dynamic paint cause", reconcileGeos, "slot-paint-cause");
forbid("runtime identity", reconcileGeos, /identical\?\s+texts/,
  "raw op-vector identity still owns text-geometry reuse");
forbid("runtime paint cause", reconcileGeos, /:else\s+:hover-paint/,
  "hover cause is hard-coded instead of carried by the paint operation");

for (const hook of [
  ":profileTargets", ":profileIdentity", ":truthText", ":truthEdit", ":capacityAppend",
  ":caretMove",
  ":textSelectionMove", ":machineSelectionMove", ":foldSet", ":pasteSet",
  ":backendSet", ":providerVariant", ":materialVariant", ":fixtureSet",
  ":profileAttention", ":originFixtureSet",
  ":deleteBlock", ":swapOrder",
]) requireToken("profile hook surface", installGround, hook);
requireToken("profile identity block ownership", profileIdentity, "block-subject-id");
requireToken("profile identity missing slots", profileIdentity, ":missing-block-slot-ids");
requireToken("profile identity auxiliary slots", profileIdentity, ":non-block-slot-ids");
requireToken("corpus exact block identity", profileHarness, "blockIdDigest");
requireToken("corpus slot conservation", profileHarness, "liveSlotConservation");
requireToken("fold product road", foldState, "reconcile!");
requireToken("origin selector uses placement anchors", profileTargets, "derived-by-anchor");

for (const row of [
  "g4-a-same-glyph-text-edit", "g4-b-hover-flip", "g4-c-caret",
  "g4-c-text-selection",
  "g4-c-machine-selection", "g4-e-backend", "g4-f-capacity",
  "g4-g-truth-death", "g4-h-vanished-slot", "g4-i-provider",
  "g4-l-oracle-positive", "g5-camera-pan", "g5-camera-zoom",
  "g5-order-only", "g5-origin-shift-cascade",
]) requireToken("retired profile row inventory", profileHarness, row);
// The named capacity-append hook is the row-(f) drive; a harness that
// recomposes the suffix itself no longer names the road it contracted for.
requireToken("row (f) capacity road", profileHarness,
  "window.__ground.capacityAppend(id, 4096)");
requireToken("paired fold rows", profileHarness, '...["noise", "paste"].map');
requireToken("paired material rows", profileHarness,
  '...["binding", "contribution", "attention"].map');
requireToken("retired profile machinery", profileHarness, "runPairedRow");
requireToken("retired profile machinery", profileHarness,
  "incomplete-retired-non-blocking");
requireToken("retired profile machinery", profileHarness, "all20RowsPassed: false");

const seededRendererScan = `(defn- position-text-op [line]
  (filter eligible? (:glyphs line)))`;
const seededClipScan = `(defn clip-result [line]
  (filter visible? (:clusters line)))`;
const seededRawIdentity = `(defn reconcile-slot-text-geos! [texts prev]
  (identical? texts (:text prev)))`;
const seededRendererRejected =
  /\(filter[\s\S]{0,500}\(:glyphs\s+line\)/.test(seededRendererScan);
const seededClipRejected =
  /\(filter[\s\S]{0,500}\(:clusters\s+line\)/.test(seededClipScan);
const seededIdentityRejected = /identical\?\s+texts/.test(seededRawIdentity);
if (!seededRendererRejected) failures.push("self-test: renderer full scan not rejected");
if (!seededClipRejected) failures.push("self-test: clip full scan not rejected");
if (!seededIdentityRejected) failures.push("self-test: raw identity not rejected");

const receipt = {
  contract: "shaping-correction/I1-I6",
  rendererIndexed: positionTextOp.includes("tl/glyphs-in-source-range"),
  clipIndexed: clipResult.includes("glyphs-in-source-range"),
  binarySpanSeek: spanSelection.includes("first-owned-span-index"),
  carriedGroundLayouts: carryGround.includes("acquire-ground-layout!"),
  materialLocalReuse:
    anatomyView.includes(":layout-result layout-result") &&
    blockLayout.includes("carried-result carried-result"),
  counterResetPreservesCache:
    resetCacheCounters.includes("assoc (or cache (empty-layout-cache))"),
  dynamicPaintCause: paintCause.includes("some :paint/cause"),
  oracleSeedRendererInert:
    seedStale.includes(":receipts :output-hash") &&
    !/assoc\s+:layout\/id/.test(seedStale),
  partitionedRuntimeIdentity:
    reconcileGeos.includes("slot-layout-token") &&
    reconcileGeos.includes("slot-paint-token"),
  retiredProfileInventoryPresent:
    profileHarness.includes("g4-a-same-glyph-text-edit") &&
    profileHarness.includes("g5-origin-shift-cascade") &&
    profileHarness.includes("incomplete-retired-non-blocking") &&
    profileHarness.includes("all20RowsPassed: false"),
  seededRendererScanRejected: seededRendererRejected,
  seededClipScanRejected: seededClipRejected,
  seededRawIdentityRejected: seededIdentityRejected,
  productionFailures: failures,
  pass: failures.length === 0,
};

console.log(`[SHAPING-CORRECTION-FENCE] ${JSON.stringify(receipt)}`);
if (!receipt.pass) process.exit(1);
