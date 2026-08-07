#!/usr/bin/env node

import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import process from "node:process";
import { execFileSync } from "node:child_process";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

import puppeteer from "puppeteer";

const require = createRequire(import.meta.url);
const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, "../..");
const buildFile = path.join(repoRoot, "target/render-verifier/js/main.js");
const actualDir = path.join(repoRoot, "target/render-verifier/actual");
const receiptFile = path.join(repoRoot, "target/render-verifier/receipt.json");
const goldenDir = path.join(
  repoRoot,
  "test/app/fixtures/render_engine/gpu-goldens",
);
const manifestFile = path.join(goldenDir, "manifest.json");
const environmentFile = path.join(goldenDir, "environment.json");
const origin = "http://localhost";
const chromeExecutable =
  process.env.RENDER_VERIFIER_CHROME || "/usr/bin/google-chrome";
const updateGoldens = process.argv.includes("--update-goldens");
const appendImageGoldens = process.argv.includes("--append-image-goldens");
const appendPathGoldens = process.argv.includes("--append-path-goldens");
const appendConnectorGoldens = process.argv.includes(
  "--append-connector-goldens",
);
const appendChromeGoldens = process.argv.includes("--append-chrome-goldens");
const appendW4Goldens = process.argv.includes("--append-w4-goldens");
const appendT2Goldens = process.argv.includes("--append-t2-goldens");
const appendRegion3dGoldens = process.argv.includes(
  "--append-region3d-goldens",
);
const amendRegion3dGizmoGolden = process.argv.includes(
  "--amend-region3d-gizmo-golden",
);
const amendT2LivedCorrection = process.argv.includes(
  "--amend-t2-lived-correction",
);
const appendImageInputAmendment = process.argv.includes(
  "--append-image-input-amendment",
);
const appendPathInputAmendment = process.argv.includes(
  "--append-path-input-amendment",
);
const appendConnectorInputAmendment = process.argv.includes(
  "--append-connector-input-amendment",
);
const appendChromeInputAmendment = process.argv.includes(
  "--append-chrome-input-amendment",
);
const appendW4InputAmendment = process.argv.includes(
  "--append-w4-input-amendment",
);
const appendT2InputAmendment = process.argv.includes(
  "--append-t2-input-amendment",
);
const amendShaderDigests = process.argv.includes("--amend-shader-digests");
const assertImageContract = process.argv.includes("--assert-image-contract");
const assertPathContract = process.argv.includes("--assert-path-contract");
const assertConnectorContract = process.argv.includes(
  "--assert-connector-contract",
);
const assertChromeContract = process.argv.includes("--assert-chrome-contract");
const assertW4Contract = process.argv.includes("--assert-w4-contract");
const assertT2Contract = process.argv.includes("--assert-t2-contract");
const assertRegion3dContract = process.argv.includes(
  "--assert-region3d-contract",
);
const launchArgs = [
  "--no-sandbox",
  "--enable-unsafe-webgpu",
  "--use-angle=swiftshader",
  "--enable-features=WebGPU,UnsafeWebGPU",
];

const sha256 = (bytes) =>
  crypto.createHash("sha256").update(bytes).digest("hex");

const sha256File = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath);
  return {
    path: relativePath,
    bytes: fs.statSync(absolutePath).size,
    sha256: sha256(fs.readFileSync(absolutePath)),
  };
};

const stableJson = (value) => {
  if (Array.isArray(value)) return value.map(stableJson);
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.keys(value)
        .sort()
        .map((key) => [key, stableJson(value[key])]),
    );
  }
  return value;
};

// WebGPU exposes adapter features as a set. Array.from preserves the browser's
// iteration order, but that order is not semantic environment identity. Keep
// ordered arrays (launch args, fallback chains, etc.) order-sensitive while
// canonicalizing this one set-valued field before it reaches receipts/hashes.
const canonicalStringSet = (values = []) =>
  [...new Set(values)].sort((left, right) =>
    left < right ? -1 : left > right ? 1 : 0,
  );

const camelizeKeys = (value) => {
  if (Array.isArray(value)) return value.map(camelizeKeys);
  if (value && Object.getPrototypeOf(value) === Object.prototype) {
    return Object.fromEntries(
      Object.entries(value).map(([key, child]) => [
        key
          .replace(/-([a-z0-9])/g, (_, segment) => segment.toUpperCase())
          .replace(/\?$/, ""),
        camelizeKeys(child),
      ]),
    );
  }
  return value;
};

const fingerprintSha = (value) =>
  sha256(Buffer.from(JSON.stringify(stableJson(value))));

const jsonRead = (file) => JSON.parse(fs.readFileSync(file, "utf8"));

const mimeType = (requestPath) => {
  if (requestPath.endsWith(".js")) return "text/javascript; charset=utf-8";
  if (requestPath.endsWith(".json")) return "application/json";
  if (requestPath.endsWith(".png")) return "image/png";
  if (requestPath.endsWith(".bin")) return "application/octet-stream";
  return "application/octet-stream";
};

const html = `<!doctype html>
<html><head><meta charset="utf-8"><title>Softland W0-A</title></head>
<body><script src="/js/main.js"></script></body></html>`;

const serveSyntheticOrigin = async (page) => {
  await page.setRequestInterception(true);
  page.on("request", (request) => {
    try {
      const url = new URL(request.url());
      if (url.origin !== origin) {
        request.abort();
        return;
      }
      if (url.pathname === "/" || url.pathname === "/index.html") {
        request.respond({
          status: 200,
          contentType: "text/html; charset=utf-8",
          body: html,
        });
        return;
      }
      if (url.pathname === "/js/main.js") {
        request.respond({
          status: 200,
          contentType: "text/javascript; charset=utf-8",
          body: fs.readFileSync(buildFile),
        });
        return;
      }
      if (url.pathname.startsWith("/fonts/")) {
        const relative = decodeURIComponent(url.pathname.slice(1));
        const absolute = path.resolve(repoRoot, "resources/public", relative);
        const fontRoot = path.resolve(repoRoot, "resources/public/fonts");
        if (!absolute.startsWith(`${fontRoot}${path.sep}`)) {
          request.abort();
          return;
        }
        request.respond({
          status: 200,
          contentType: mimeType(absolute),
          body: fs.readFileSync(absolute),
        });
        return;
      }
      if (url.pathname.startsWith("/images/")) {
        const filename = decodeURIComponent(
          url.pathname.slice("/images/".length),
        );
        const imageRoot = path.resolve(
          repoRoot,
          "test/app/fixtures/render_engine/images",
        );
        const absolute = path.resolve(imageRoot, filename);
        if (!absolute.startsWith(`${imageRoot}${path.sep}`)) {
          request.abort();
          return;
        }
        request.respond({
          status: 200,
          contentType: "image/png",
          body: fs.readFileSync(absolute),
        });
        return;
      }
      request.respond({ status: 404, body: "not found" });
    } catch (error) {
      request.abort();
    }
  });
};

const productionInputs = () => ({
  rendererSource: sha256File(
    "src/app/client/substrate/webgpu/renderer.cljs",
  ),
  msdfAtlas: sha256File(
    "resources/public/fonts/dejavu_sans_mono_atlas.png",
  ),
  msdfMetrics: sha256File(
    "resources/public/fonts/dejavu_sans_mono_atlas.json",
  ),
  slugMeta: sha256File(
    "resources/public/fonts/dejavu_sans_mono_slug_meta.json",
  ),
  slugCurves: sha256File(
    "resources/public/fonts/dejavu_sans_mono_slug_curve.bin",
  ),
  slugBands: sha256File(
    "resources/public/fonts/dejavu_sans_mono_slug_band.bin",
  ),
  fontManifest: sha256File("resources/public/fonts/manifest.json"),
});

const stripDataUrls = (result) => ({
  ...result,
  cases: result.cases.map((renderCase) => ({
    ...renderCase,
    images: renderCase.images.map(({ pngDataUrl, ...image }) => image),
  })),
  imageAtom: {
    ...result.imageAtom,
    cases: result.imageAtom.cases.map((renderCase) => ({
      ...renderCase,
      images: renderCase.images.map(({ pngDataUrl, ...image }) => image),
    })),
  },
  pathAtom: {
    ...result.pathAtom,
    cases: result.pathAtom.cases.map((renderCase) => ({
      ...renderCase,
      images: renderCase.images.map(({ pngDataUrl, ...image }) => image),
    })),
  },
  connectorAtom: {
    ...result.connectorAtom,
    cases: result.connectorAtom.cases.map((renderCase) => ({
      ...renderCase,
      images: renderCase.images.map(({ pngDataUrl, ...image }) => image),
    })),
  },
  chromeAtom: {
    ...result.chromeAtom,
    cases: result.chromeAtom.cases.map((renderCase) => ({
      ...renderCase,
      images: renderCase.images.map(({ pngDataUrl, ...image }) => image),
    })),
  },
  w4FrameRuntime: {
    ...result.w4FrameRuntime,
    cases: result.w4FrameRuntime.cases.map((renderCase) => ({
      ...renderCase,
      images: renderCase.images.map(({ pngDataUrl, ...image }) => image),
    })),
  },
  t2InputFloor: {
    ...result.t2InputFloor,
    cases: result.t2InputFloor.cases.map((renderCase) => ({
      ...renderCase,
      images: renderCase.images.map(({ pngDataUrl, ...image }) => image),
    })),
  },
  region3dFloor: {
    ...result.region3dFloor,
    cases: result.region3dFloor.cases.map((renderCase) => ({
      ...renderCase,
      images: renderCase.images.map(({ pngDataUrl, ...image }) => image),
    })),
  },
});

const imageRows = (result) =>
  result.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.regime,
      normalization: renderCase.normalization,
      shapeExtentWorld: renderCase.shapeExtentWorld,
      mode: image.mode,
      file: image.file,
      rawSha256: image.rawSha256,
      pngSha256: sha256(
        Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64"),
      ),
    })),
  );

const imageAtomRows = (result) =>
  result.imageAtom.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.regime,
      normalization: renderCase.normalization,
      shapeExtentWorld: renderCase.shapeExtentWorld,
      mode: image.mode,
      file: image.file,
      rawSha256: image.rawSha256,
      pngSha256: sha256(
        Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64"),
      ),
    })),
  );

const imageAtomInputs = () => ({
  sceneTape: sha256File("src/app/client/substrate/scene_tape.cljc"),
  sceneStore: sha256File("src/app/client/workspace/scene_store.cljc"),
  rectTree: sha256File("src/app/client/workspace/rect_tree.cljc"),
  verifier: sha256File("src/app/client/substrate/webgpu/verifier.cljs"),
});

const pathAtomRows = (result) =>
  result.pathAtom.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.regime,
      normalization: renderCase.normalization,
      shapeExtentWorld: renderCase.shapeExtentWorld,
      mode: image.mode,
      file: image.file,
      rawSha256: image.rawSha256,
      pngSha256: sha256(
        Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64"),
      ),
    })),
  );

const pathAtomInputs = () => ({
  pathMaterial: sha256File("src/app/client/substrate/path_material.cljc"),
  pathTessellation: sha256File(
    "src/app/client/substrate/path_tessellation.cljc",
  ),
  pathGpu: sha256File("src/app/client/substrate/webgpu/path_gpu.cljs"),
  sceneTape: sha256File("src/app/client/substrate/scene_tape.cljc"),
  sceneStore: sha256File("src/app/client/workspace/scene_store.cljc"),
  rectTree: sha256File("src/app/client/workspace/rect_tree.cljc"),
  liveAtoms: sha256File("src/app/client/workspace/live_atoms.cljs"),
  electricFlow: sha256File("src/app/electric_flow.cljc"),
  runtimeRender: sha256File("src/app/client/workspace/runtime/render.cljs"),
  renderer: sha256File("src/app/client/substrate/webgpu/renderer.cljs"),
  verifier: sha256File("src/app/client/substrate/webgpu/verifier.cljs"),
});

const connectorAtomRows = (result) =>
  result.connectorAtom.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.regime,
      edgeCount: renderCase.edgeCount,
      mode: image.mode,
      file: image.file,
      rawSha256: image.rawSha256,
      pngSha256: sha256(
        Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64"),
      ),
    })),
  );

const connectorAtomInputs = () => ({
  connectorMaterial: sha256File(
    "src/app/client/substrate/connector_material.cljc",
  ),
  connectorRoute: sha256File(
    "src/app/client/substrate/connector_route.cljc",
  ),
  connectorGpu: sha256File(
    "src/app/client/substrate/webgpu/connector_gpu.cljs",
  ),
  sceneTape: sha256File("src/app/client/substrate/scene_tape.cljc"),
  sceneStore: sha256File("src/app/client/workspace/scene_store.cljc"),
  rectTree: sha256File("src/app/client/workspace/rect_tree.cljc"),
  liveAtoms: sha256File("src/app/client/workspace/live_atoms.cljs"),
  liveEdges: sha256File("src/app/client/workspace/live_edges.cljc"),
  fileViewer: sha256File("src/app/file_viewer.cljc"),
  electricFlow: sha256File("src/app/electric_flow.cljc"),
  runtimeRender: sha256File("src/app/client/workspace/runtime/render.cljs"),
  renderer: sha256File("src/app/client/substrate/webgpu/renderer.cljs"),
  verifier: sha256File("src/app/client/substrate/webgpu/verifier.cljs"),
});

const chromeAtomRows = (result) =>
  result.chromeAtom.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.regime,
      normalization: renderCase.normalization,
      shapeExtentWorld: renderCase.shapeExtentWorld,
      formCount: renderCase.formCount,
      mode: image.mode,
      file: image.file,
      rawSha256: image.rawSha256,
      pngSha256: sha256(
        Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64"),
      ),
    })),
  );

const chromeSourceFiles = {
  selection: "src/app/client/workspace/selection.cljc",
  chromeMaterial: "src/app/client/substrate/chrome_material.cljc",
  snap: "src/app/client/substrate/snap.cljc",
  chromeDerive: "src/app/client/substrate/chrome_derive.cljc",
  chromeGpu: "src/app/client/substrate/webgpu/chrome_gpu.cljs",
  chromeRuntime: "src/app/client/workspace/chrome_runtime.cljs",
};

const chromeAtomInputs = () => ({
  ...Object.fromEntries(
    Object.entries(chromeSourceFiles).map(([key, relativePath]) => [
      key,
      sha256File(relativePath),
    ]),
  ),
  sceneTape: sha256File("src/app/client/substrate/scene_tape.cljc"),
  sceneStore: sha256File("src/app/client/workspace/scene_store.cljc"),
  rectTree: sha256File("src/app/client/workspace/rect_tree.cljc"),
  sceneRuntime: sha256File("src/app/client/workspace/scene_runtime.cljs"),
  ground: sha256File("src/app/client/workspace/ground.cljs"),
  liveAtoms: sha256File("src/app/client/workspace/live_atoms.cljs"),
  electricFlow: sha256File("src/app/electric_flow.cljc"),
  runtimeRender: sha256File("src/app/client/workspace/runtime/render.cljs"),
  renderer: sha256File("src/app/client/substrate/webgpu/renderer.cljs"),
  verifier: sha256File("src/app/client/substrate/webgpu/verifier.cljs"),
  runner: sha256File("test/render_engine/run_verifier.mjs"),
});

const w4FrameRuntimeRows = (result) =>
  result.w4FrameRuntime.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.regime,
      normalization: renderCase.normalization,
      shapeExtentWorld: renderCase.shapeExtentWorld,
      mode: image.mode,
      file: image.file,
      rawSha256: image.rawSha256,
      pngSha256: sha256(
        Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64"),
      ),
    })),
  );

const w4FrameRuntimeInputs = () => ({
  frameEffects: sha256File("src/app/client/substrate/frame_effects.cljc"),
  frameGraph: sha256File("src/app/client/substrate/frame_graph.cljc"),
  frameScheduler: sha256File("src/app/client/substrate/frame_scheduler.cljc"),
  compositorGpu: sha256File(
    "src/app/client/substrate/webgpu/compositor_gpu.cljs",
  ),
  frameRuntime: sha256File("src/app/client/workspace/frame_runtime.cljs"),
  sceneTape: sha256File("src/app/client/substrate/scene_tape.cljc"),
  sceneStore: sha256File("src/app/client/workspace/scene_store.cljc"),
  rectTree: sha256File("src/app/client/workspace/rect_tree.cljc"),
  containers: sha256File("src/app/client/workspace/containers.cljc"),
  sceneRuntime: sha256File("src/app/client/workspace/scene_runtime.cljs"),
  chromeMaterial: sha256File("src/app/client/substrate/chrome_material.cljc"),
  chromeGpu: sha256File("src/app/client/substrate/webgpu/chrome_gpu.cljs"),
  liveAtoms: sha256File("src/app/client/workspace/live_atoms.cljs"),
  electricFlow: sha256File("src/app/electric_flow.cljc"),
  runtimeRender: sha256File("src/app/client/workspace/runtime/render.cljs"),
  renderer: sha256File("src/app/client/substrate/webgpu/renderer.cljs"),
  verifier: sha256File("src/app/client/substrate/webgpu/verifier.cljs"),
  runner: sha256File("test/render_engine/run_verifier.mjs"),
  fence: sha256File("test/render_engine/verify_scene_tape_fence.mjs"),
});

const t2InputFloorRows = (result) =>
  result.t2InputFloor.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.regime,
      normalization: renderCase.normalization,
      shapeExtentWorld: renderCase.shapeExtentWorld,
      mode: image.mode,
      file: image.file,
      rawSha256: image.rawSha256,
      pngSha256: sha256(
        Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64"),
      ),
    })),
  );

const t2InputFloorInputs = () => ({
  textEditing: sha256File("src/app/client/workspace/text_editing.cljc"),
  editingSegmentation: sha256File(
    "src/app/client/workspace/editing_segmentation.cljs",
  ),
  editingRuntime: sha256File(
    "src/app/client/workspace/editing_runtime.cljs",
  ),
  textLayout: sha256File("src/app/client/workspace/text_layout.cljc"),
  events: sha256File("src/app/client/workspace/events.cljs"),
  runtimeMouse: sha256File("src/app/client/workspace/runtime/mouse.cljs"),
  runtimeRender: sha256File("src/app/client/workspace/runtime/render.cljs"),
  liveAtoms: sha256File("src/app/client/workspace/live_atoms.cljs"),
  frameScheduler: sha256File(
    "src/app/client/substrate/frame_scheduler.cljc",
  ),
  sceneStore: sha256File("src/app/client/workspace/scene_store.cljc"),
  sceneRuntime: sha256File("src/app/client/workspace/scene_runtime.cljs"),
  rectTree: sha256File("src/app/client/workspace/rect_tree.cljc"),
  containers: sha256File("src/app/client/workspace/containers.cljc"),
  renderer: sha256File("src/app/client/substrate/webgpu/renderer.cljs"),
  verifier: sha256File("src/app/client/substrate/webgpu/verifier.cljs"),
  runner: sha256File("test/render_engine/run_verifier.mjs"),
  fence: sha256File("test/render_engine/verify_text_layout_fence.mjs"),
});

const region3dFloorRows = (result) =>
  result.region3dFloor.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      mode: image.mode,
      file: image.file,
      rawSha256: image.rawSha256,
      pngSha256: sha256(
        Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64"),
      ),
    })),
  );

const region3dFloorInputs = () => ({
  region3dMaterial: sha256File(
    "src/app/client/substrate/region3d_material.cljc",
  ),
  region3dScene: sha256File("src/app/client/substrate/region3d_scene.cljc"),
  region3dGpu: sha256File(
    "src/app/client/substrate/webgpu/region3d_gpu.cljs",
  ),
  region3dRuntime: sha256File(
    "src/app/client/workspace/region3d_runtime.cljs",
  ),
  compositorGpu: sha256File(
    "src/app/client/substrate/webgpu/compositor_gpu.cljs",
  ),
  frameGraph: sha256File("src/app/client/substrate/frame_graph.cljc"),
  sceneTape: sha256File("src/app/client/substrate/scene_tape.cljc"),
  rectTree: sha256File("src/app/client/workspace/rect_tree.cljc"),
  sceneStore: sha256File("src/app/client/workspace/scene_store.cljc"),
  sceneRuntime: sha256File("src/app/client/workspace/scene_runtime.cljs"),
  runtimeRender: sha256File("src/app/client/workspace/runtime/render.cljs"),
  renderer: sha256File("src/app/client/substrate/webgpu/renderer.cljs"),
  verifier: sha256File("src/app/client/substrate/webgpu/verifier.cljs"),
  runner: sha256File("test/render_engine/run_verifier.mjs"),
});

const t2NewNamespaceFiles = [
  "src/app/client/workspace/text_editing.cljc",
  "src/app/client/workspace/editing_segmentation.cljs",
  "src/app/client/workspace/editing_runtime.cljs",
];

const t2StaticAbsence = () => {
  const executionClockTokens = t2NewNamespaceFiles.flatMap((relativePath) => {
    const source = fs.readFileSync(path.join(repoRoot, relativePath), "utf8");
    return [
      ...source.matchAll(
        /(?:js\/Date|performance\.now|requestAnimationFrame|\brAF\b|setInterval|setTimeout)/g,
      ),
    ].map((match) => ({ path: relativePath, token: match[0] }));
  });
  return {
    namespaceCount: t2NewNamespaceFiles.length,
    namespaces: t2NewNamespaceFiles,
    executionClockTokens,
    pass:
      t2NewNamespaceFiles.length === 3 && executionClockTokens.length === 0,
  };
};

const sourceTokenRows = (pattern) =>
  Object.entries(chromeSourceFiles).flatMap(([namespace, relativePath]) => {
    const source = fs.readFileSync(path.join(repoRoot, relativePath), "utf8");
    return [...source.matchAll(pattern)].map((match) => ({
      namespace,
      path: relativePath,
      token: match[0],
    }));
  });

const chromeStaticAbsence = () => {
  const textTokens = sourceTokenRows(
    /\b(?:text-layout|font|msdf|slug(?:-layout)?)\b/gi,
  );
  const persistenceTokens = sourceTokenRows(
    /\b(?:fetch|localStorage|indexedDB|foreign-append!)\b/gi,
  );
  const executionClockTokens = sourceTokenRows(
    /\b(?:requestAnimationFrame|setInterval|js\/Date)\b/g,
  );
  return {
    namespaceCount: Object.keys(chromeSourceFiles).length,
    namespaces: Object.keys(chromeSourceFiles),
    textTokens,
    persistenceTokens,
    executionClockTokens,
    pass:
      Object.keys(chromeSourceFiles).length === 6 &&
      textTokens.length === 0 &&
      persistenceTokens.length === 0 &&
      executionClockTokens.length === 0,
  };
};

const writeActualImages = (result) => {
  fs.mkdirSync(actualDir, { recursive: true });
  for (const renderCase of result.cases) {
    for (const image of renderCase.images) {
      const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
      fs.writeFileSync(path.join(actualDir, image.file), png);
    }
  }
  for (const renderCase of result.imageAtom.cases) {
    for (const image of renderCase.images) {
      const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
      fs.writeFileSync(path.join(actualDir, image.file), png);
    }
  }
  for (const renderCase of result.pathAtom.cases) {
    for (const image of renderCase.images) {
      const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
      fs.writeFileSync(path.join(actualDir, image.file), png);
    }
  }
  for (const renderCase of result.connectorAtom.cases) {
    for (const image of renderCase.images) {
      const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
      fs.writeFileSync(path.join(actualDir, image.file), png);
    }
  }
  for (const renderCase of result.chromeAtom.cases) {
    for (const image of renderCase.images) {
      const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
      fs.writeFileSync(path.join(actualDir, image.file), png);
    }
  }
  for (const renderCase of result.w4FrameRuntime.cases) {
    for (const image of renderCase.images) {
      const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
      fs.writeFileSync(path.join(actualDir, image.file), png);
    }
  }
  for (const renderCase of result.t2InputFloor.cases) {
    for (const image of renderCase.images) {
      const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
      fs.writeFileSync(path.join(actualDir, image.file), png);
    }
  }
  for (const renderCase of result.region3dFloor.cases) {
    for (const image of renderCase.images) {
      const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
      fs.writeFileSync(path.join(actualDir, image.file), png);
    }
  }
};

const parityRows = (result) =>
  result.cases.flatMap((renderCase) =>
    renderCase.pickParity.map((row) => ({
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.regime,
      ...row,
    })),
  );

const determinismRows = (result) =>
  result.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.regime,
      mode: image.mode,
      ...image.determinism,
    })),
  );

const imageAtomDeterminismRows = (result) =>
  result.imageAtom.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.regime,
      mode: image.mode,
      ...image.determinism,
    })),
  );

const pathAtomDeterminismRows = (result) =>
  result.pathAtom.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.regime,
      mode: image.mode,
      ...image.determinism,
    })),
  );

const connectorAtomDeterminismRows = (result) =>
  result.connectorAtom.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.regime,
      mode: image.mode,
      ...image.determinism,
    })),
  );

const chromeAtomDeterminismRows = (result) =>
  result.chromeAtom.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.regime,
      mode: image.mode,
      ...image.determinism,
    })),
  );

const w4FrameRuntimeDeterminismRows = (result) =>
  result.w4FrameRuntime.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.regime,
      mode: image.mode,
      ...image.determinism,
    })),
  );

const t2InputFloorDeterminismRows = (result) =>
  result.t2InputFloor.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.regime,
      mode: image.mode,
      ...image.determinism,
    })),
  );

const region3dFloorDeterminismRows = (result) =>
  result.region3dFloor.cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({
      caseId: renderCase.caseId,
      mode: image.mode,
      ...image.determinism,
    })),
  );

const expectedDivergenceRows = (result) =>
  result.cases.map((renderCase) => ({
    caseId: renderCase.caseId,
    zoom: renderCase.zoom,
    regime: renderCase.regime,
    ...renderCase.currentProductPickSentinel,
  }));

const main = async () => {
  if (updateGoldens) {
    throw new Error(
      "--update-goldens is forbidden; use a scoped atom append road",
    );
  }
  if (
    [
      appendImageGoldens,
      appendPathGoldens,
      appendConnectorGoldens,
      appendChromeGoldens,
      appendW4Goldens,
      appendT2Goldens,
      appendRegion3dGoldens,
      amendRegion3dGizmoGolden,
      amendT2LivedCorrection,
      appendImageInputAmendment,
      appendPathInputAmendment,
      appendConnectorInputAmendment,
      appendChromeInputAmendment,
      appendW4InputAmendment,
      appendT2InputAmendment,
      amendShaderDigests,
    ].filter(Boolean).length > 1
  ) {
    throw new Error("Only one scoped atom append may run at a time");
  }
  if (!fs.existsSync(buildFile)) {
    throw new Error(
      `Missing verifier build ${path.relative(repoRoot, buildFile)}; use npm run verify:render-engine`,
    );
  }

  const browser = await puppeteer.launch({
    executablePath: chromeExecutable,
    headless: "chrome",
    ignoreHTTPSErrors: true,
    args: launchArgs,
  });
  await browser.defaultBrowserContext().overridePermissions(origin, [
    "clipboard-read",
    "clipboard-write",
  ]);
  const page = await browser.newPage();
  const browserSession = await page.target().createCDPSession();
  await browserSession.send("Browser.grantPermissions", {
    origin,
    permissions: ["clipboardReadWrite", "clipboardSanitizedWrite"],
  });
  for (const name of ["clipboard-read", "clipboard-write"]) {
    await browserSession.send("Browser.setPermission", {
      origin,
      permission: { name, allowWithoutSanitization: true },
      setting: "granted",
    });
  }
  const browserConsole = [];
  page.on("console", (message) => {
    const row = { type: message.type(), text: message.text() };
    browserConsole.push(row);
    if (row.text.startsWith("[W0-A]")) console.log(row.text);
  });
  page.on("pageerror", (error) => {
    const row = { type: "pageerror", text: String(error) };
    browserConsole.push(row);
    console.error("[W0-A] pageerror", row.text);
  });

  let result;
  try {
    await serveSyntheticOrigin(page);
    await page.goto(`${origin}/`, { waitUntil: "load", timeout: 30_000 });
    await page.waitForFunction(
      () =>
        window.__softlandT2TrustedCopyReady === true ||
        window.__renderVerifierDone === true,
      { timeout: 600_000 },
    );
    if (
      await page.evaluate(
        () => window.__softlandT2TrustedCopyReady === true,
      )
    ) {
      await page.keyboard.down("Control");
      await page.keyboard.press("KeyC");
      await page.keyboard.up("Control");
      await page.waitForFunction(
        () => window.__softlandT2TrustedCopyDone === true,
        { timeout: 30_000 },
      );
    }
    await page.waitForFunction(() => window.__renderVerifierDone === true, {
      // SwiftShader performs 49 production-pipeline render/readback operations.
      // This is a replay ceiling, not a performance verdict.
      timeout: 600_000,
    });
    result = camelizeKeys(
      await page.evaluate(() => window.__renderVerifierResult),
    );
  } catch (error) {
    console.error("W0-A browser progress before failure:");
    console.error(
      browserConsole
        .filter((row) => row.text.startsWith("[W0-A]") || row.type === "pageerror")
        .slice(-40),
    );
    throw error;
  } finally {
    await browser.close();
  }

  if (!result || result.fatal) {
    throw new Error(
      `Browser verifier failed: ${result?.fatal || "missing result"}\n${result?.stack || ""}`,
    );
  }

  const inputs = productionInputs();
  const {
    isFallbackAdapter,
    fallbackAttestationSource,
    renderer: adapterRenderer,
    ...fingerprintAdapter
  } = result.adapter;
  const attestation = {
    adapterIdentity: {
      vendor: result.adapter.vendor,
      architecture: result.adapter.architecture,
      device: result.adapter.device,
    },
    isFallbackAdapter,
    fallbackAttestationSource,
    renderer: adapterRenderer,
  };
  const environment = {
    os: {
      platform: os.platform(),
      architecture: os.arch(),
      kernelRelease: os.release(),
    },
    runtime: {
      node: process.version,
      puppeteer: require("puppeteer/package.json").version,
      chromeExecutable,
      chromeVersion: execFileSync(chromeExecutable, ["--version"], {
        encoding: "utf8",
      }).trim(),
      launchArgs,
    },
    browser: {
      secureContext: result.secureContext,
      userAgent: result.userAgent,
      adapter: {
        ...fingerprintAdapter,
        features: canonicalStringSet(fingerprintAdapter?.features),
      },
      deviceLimits: result.deviceLimits,
      canvas: result.canvas,
    },
  };
  environment.fingerprintSha256 = fingerprintSha(environment);

  const currentManifest = {
    schemaVersion: 1,
    verifier: result.verifier,
    captureAuthority:
      "actual WebGPU render through production pipelines; SwiftShader is software GPU, not hardware",
    productPickAuthority:
      "candidate CPU point-in-path contract only; current product pick remains axis-aligned bounds",
    shaderDigests: result.shaderDigests,
    productionInputs: inputs,
    images: imageRows(result),
    imageAtomInputs: imageAtomInputs(),
    imageAtomCases: imageAtomRows(result),
    pathAtomInputs: pathAtomInputs(),
    pathAtomCases: pathAtomRows(result),
    connectorAtomInputs: connectorAtomInputs(),
    connectorAtomCases: connectorAtomRows(result),
    chromeAtomInputs: chromeAtomInputs(),
    chromeAtomCases: chromeAtomRows(result),
    w4FrameRuntimeInputs: w4FrameRuntimeInputs(),
    w4FrameRuntimeCases: w4FrameRuntimeRows(result),
    t2InputFloorInputs: t2InputFloorInputs(),
    t2InputFloorCases: t2InputFloorRows(result),
    region3dFloorInputs: region3dFloorInputs(),
    region3dFloorCases: region3dFloorRows(result),
  };
  const deterministic = determinismRows(result);
  const imageDeterministic = imageAtomDeterminismRows(result);
  const pathDeterministic = pathAtomDeterminismRows(result);
  const connectorDeterministic = connectorAtomDeterminismRows(result);
  const chromeDeterministic = chromeAtomDeterminismRows(result);
  const w4Deterministic = w4FrameRuntimeDeterminismRows(result);
  const t2Deterministic = t2InputFloorDeterminismRows(result);
  const region3dDeterministic = region3dFloorDeterminismRows(result);
  const chromeAbsence = chromeStaticAbsence();
  const t2Absence = t2StaticAbsence();
  const imageParity = result.imageAtom.parity;
  const pathParity = result.pathAtom.parity;
  const connectorParity = result.connectorAtom.parity;
  const parity = parityRows(result);
  const divergences = expectedDivergenceRows(result);
  const determinismPass = deterministic.every((row) => row.byteIdentical);
  const imageDeterminismPass =
    imageDeterministic.length === 21 &&
    imageDeterministic.every((row) => row.byteIdentical);
  const imageParityPass =
    imageParity.length === 14 &&
    imageParity.every(
      (row) =>
        row.pass && row.boundaryPixelCount > 0 && row.decisiveCount > 0,
    );
  const pathDeterminismPass =
    pathDeterministic.length === 3 &&
    pathDeterministic.every((row) => row.byteIdentical);
  const pathParityPass =
    pathParity.length === 7 &&
    pathParity.every(
      (row) =>
        row.pass &&
        row.boundaryPixelCount > 0 &&
        row.decisiveCount > 0 &&
        row.mismatchCount === 0,
    );
  const connectorDeterminismPass =
    connectorDeterministic.length === 3 &&
    connectorDeterministic.every((row) => row.byteIdentical);
  const chromeDeterminismPass =
    chromeDeterministic.length === 3 &&
    chromeDeterministic.every((row) => row.byteIdentical);
  const w4DeterminismPass =
    w4Deterministic.length === 3 &&
    w4Deterministic.every((row) => row.byteIdentical);
  const t2DeterminismPass =
    t2Deterministic.length === 3 &&
    t2Deterministic.every((row) => row.byteIdentical);
  const region3dDeterminismPass =
    region3dDeterministic.length === 3 &&
    region3dDeterministic.every((row) => row.byteIdentical);
  const connectorParityPass =
    connectorParity.length === 7 &&
    connectorParity.every(
      (row) =>
        row.pass &&
        row.boundaryPixelCount > 0 &&
        row.decisiveCount > 0 &&
        row.mismatchCount === 0,
    );
  const parityPass = parity.every((row) => row.pass);
  const divergencePass = divergences.every((row) => row.expectedDivergence);
  const q8TransportPass = Boolean(
    result.q8Transport?.pass &&
      result.q8Transport.rows?.length === 3 &&
      result.q8Transport.rows.every((row) => row.pass),
  );
  const q5AffineBoundaryPass = Boolean(
    result.q5AffineBoundary?.pass &&
      result.q5AffineBoundary.rows?.length === 2 &&
      result.q5AffineBoundary.rows.every(
        (row) => row.pass && row.cpuClass === "inside" && row.gpuCoverageByte > 0,
      ),
  );
  // Pixel-bank custody and geometry-contract parity are independent receipts.
  // A deterministic production image remains worth banking when candidate
  // point-in-path parity is red; the replay must still exit red below and must
  // never turn that disagreement into an accepted geometry baseline.
  const updateAuthorized =
    determinismPass && divergencePass && q8TransportPass && q5AffineBoundaryPass;

  Object.assign(currentManifest, {
    replayCommand: "npm run verify:render-engine",
    environmentFingerprintSha256: environment.fingerprintSha256,
    baselineReceipts: {
      q8AffineTransport: result.q8Transport,
      q5AffineRasterBoundary: result.q5AffineBoundary,
      determinism: {
        pass: determinismPass,
        rows: deterministic,
      },
      candidatePickParity: {
        pass: parityPass,
        boundaryTieSemantics:
          "rgba8unorm byte 128 is recorded as an indeterminate boundary tie, never silently coerced to inside or outside",
        currentProductPick: false,
        rows: parity,
      },
      currentProductBoundsPickDivergence: {
        pass: divergencePass,
        rows: divergences,
      },
    },
  });

  writeActualImages(result);

  const bankPresent =
    fs.existsSync(manifestFile) && fs.existsSync(environmentFile);
  let expectedManifest = bankPresent ? jsonRead(manifestFile) : null;
  const expectedEnvironment = bankPresent ? jsonRead(environmentFile) : null;
  const environmentMatch =
    bankPresent &&
    expectedEnvironment.fingerprintSha256 === environment.fingerprintSha256;

  const compareGoldenRows = (currentRows, expectedRows) => {
    const expectedByFile = new Map(
      (expectedRows || []).map((image) => [image.file, image]),
    );
    return currentRows.map((image) => {
      const expected = expectedByFile.get(image.file);
      const goldenFile = path.join(goldenDir, image.file);
      const goldenPngSha256 = fs.existsSync(goldenFile)
        ? sha256(fs.readFileSync(goldenFile))
        : null;
      return {
        ...image,
        expectedRawSha256: expected?.rawSha256 || null,
        expectedPngSha256: expected?.pngSha256 || null,
        goldenPngSha256,
        rawMatch: Boolean(expected && expected.rawSha256 === image.rawSha256),
        pngManifestMatch: Boolean(
          expected && expected.pngSha256 === image.pngSha256,
        ),
        goldenFileMatch: Boolean(
          expected && expected.pngSha256 === goldenPngSha256,
        ),
      };
    });
  };

  const comparisonPass = (rows, expectedRows, exactCount) =>
    rows.length === exactCount &&
    (expectedRows || []).length === exactCount &&
    rows.every(
      (row) => row.rawMatch && row.pngManifestMatch && row.goldenFileMatch,
    );

  let imageComparison = compareGoldenRows(
    currentManifest.images,
    expectedManifest?.images,
  );
  let imagePass =
    bankPresent &&
    comparisonPass(imageComparison, expectedManifest?.images, 21);

  const legacyFilenameSetExact =
    new Set(currentManifest.images.map((row) => row.file)).size === 21 &&
    new Set(expectedManifest?.images?.map((row) => row.file) || []).size === 21 &&
    currentManifest.images.every((row) =>
      expectedManifest?.images?.some((expected) => expected.file === row.file),
    );
  const productionInputDebtKeys = Object.keys(
    currentManifest.productionInputs,
  )
    .filter(
      (key) =>
        fingerprintSha(expectedManifest?.productionInputs?.[key]) !==
        fingerprintSha(currentManifest.productionInputs[key]),
    )
    .sort();
  const shaderDigestKeysExact =
    JSON.stringify(Object.keys(expectedManifest?.shaderDigests || {}).sort()) ===
    JSON.stringify(Object.keys(currentManifest.shaderDigests).sort());
  const knownSourceDebtOnly =
    bankPresent &&
    shaderDigestKeysExact &&
    JSON.stringify(productionInputDebtKeys) ===
      JSON.stringify(["fontManifest", "rendererSource"]);
  const rendererPackageDebtOnly =
    bankPresent &&
    shaderDigestKeysExact &&
    JSON.stringify(productionInputDebtKeys) ===
      JSON.stringify(["rendererSource"]);
  const sourceMetadataCurrent =
    bankPresent &&
    fingerprintSha(expectedManifest.shaderDigests) ===
      fingerprintSha(currentManifest.shaderDigests) &&
    fingerprintSha(expectedManifest.productionInputs) ===
      fingerprintSha(currentManifest.productionInputs);

  const appendPreflight = {
    legacyFilenameSetExact,
    existingGoldenRows: imageComparison.length,
    existingGoldensPass: imagePass,
    legacyDeterminismPass:
      deterministic.length === 21 && determinismPass,
    environmentMatch,
    oldPngDigestsUnchanged: imageComparison.every(
      (row) => row.goldenFileMatch,
    ),
    knownSourceDebtOnly,
    rendererPackageDebtOnly,
    sourceMetadataCurrent,
    shaderDigestKeysExact,
    manifestShaderDigests: expectedManifest?.shaderDigests,
    currentShaderDigests: currentManifest.shaderDigests,
    sourceMetadataDebtKeys: productionInputDebtKeys,
    fontManifestDebt: {
      manifest: expectedManifest?.productionInputs?.fontManifest?.sha256,
      current: currentManifest.productionInputs.fontManifest.sha256,
    },
    rendererSourceDebt: {
      manifest: expectedManifest?.productionInputs?.rendererSource?.sha256,
      current: currentManifest.productionInputs.rendererSource.sha256,
    },
    imageRows: currentManifest.imageAtomCases.length,
    imageDeterminismPass,
    imageParityPass,
    imageContractPass: result.imageAtom.pass,
  };
  // IMAGE-ATOM T7: the package has a scoped append road, never a bulk golden
  // blessing road; legacy filenames, bytes, determinism, and environment must
  // all preflight before only the separately named image rows are admitted.
  const appendAuthorized =
    legacyFilenameSetExact &&
    imagePass &&
    deterministic.length === 21 &&
    determinismPass &&
    environmentMatch &&
    imageComparison.every((row) => row.goldenFileMatch) &&
    (knownSourceDebtOnly || rendererPackageDebtOnly || sourceMetadataCurrent) &&
    currentManifest.imageAtomCases.length === 21 &&
    imageDeterminismPass &&
    imageParityPass &&
    result.imageAtom.pass;

  if (appendImageGoldens) {
    if (!appendAuthorized) {
      throw new Error(
        `IMAGE-ATOM append preflight failed: ${JSON.stringify(appendPreflight)}`,
      );
    }
    const legacyFiles = new Set(expectedManifest.images.map((row) => row.file));
    const imageFiles = currentManifest.imageAtomCases.map((row) => row.file);
    if (
      new Set(imageFiles).size !== 21 ||
      imageFiles.some((file) => legacyFiles.has(file))
    ) {
      throw new Error("IMAGE-ATOM append filenames collide or are not unique");
    }
    for (const renderCase of result.imageAtom.cases) {
      for (const image of renderCase.images) {
        const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
        fs.writeFileSync(path.join(goldenDir, image.file), png);
      }
    }
    expectedManifest = {
      ...expectedManifest,
      shaderDigests: currentManifest.shaderDigests,
      productionInputs: currentManifest.productionInputs,
      imageAtomInputs: currentManifest.imageAtomInputs,
      imageAtomCases: currentManifest.imageAtomCases,
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
    imageComparison = compareGoldenRows(
      currentManifest.images,
      expectedManifest.images,
    );
    imagePass = comparisonPass(imageComparison, expectedManifest.images, 21);
  }

  const imageAtomComparisonBeforePathAppend = compareGoldenRows(
    currentManifest.imageAtomCases,
    expectedManifest?.imageAtomCases,
  );
  const imageAtomPassBeforePathAppend = comparisonPass(
    imageAtomComparisonBeforePathAppend,
    expectedManifest?.imageAtomCases,
    21,
  );
  const pathAppendPreflight = {
    bankPresent,
    legacyFilenameSetExact,
    legacyGoldensPass: imagePass,
    imageGoldensPass: imageAtomPassBeforePathAppend,
    environmentMatch,
    legacyDeterminismPass:
      deterministic.length === 21 && determinismPass,
    imageDeterminismPass,
    imageParityPass,
    imageContractPass: result.imageAtom.pass,
    existingPathRows: expectedManifest?.pathAtomCases?.length || 0,
    pathRows: currentManifest.pathAtomCases.length,
    pathDeterminismPass,
    pathParityPass,
    pathColorPass: result.pathAtom.color?.pass,
    pathArrangementPass: result.pathAtom.arrangement?.pass,
    pathDarkLanePass: result.pathAtom.darkLane?.pass,
    pathUploadGatePass: result.pathAtom.uploadGate?.pass,
    pathContractPass: result.pathAtom.pass,
  };
  const pathAppendAuthorized =
    bankPresent &&
    legacyFilenameSetExact &&
    imagePass &&
    imageAtomPassBeforePathAppend &&
    environmentMatch &&
    deterministic.length === 21 &&
    determinismPass &&
    imageDeterminismPass &&
    imageParityPass &&
    result.imageAtom.pass &&
    !expectedManifest.pathAtomCases &&
    currentManifest.pathAtomCases.length === 3 &&
    pathDeterminismPass &&
    pathParityPass &&
    result.pathAtom.color?.pass &&
    result.pathAtom.arrangement?.pass &&
    result.pathAtom.darkLane?.pass &&
    result.pathAtom.uploadGate?.pass &&
    result.pathAtom.pass;

  if (appendPathGoldens) {
    if (!pathAppendAuthorized) {
      throw new Error(
        `PATH-ATOM append preflight failed: ${JSON.stringify(pathAppendPreflight)}`,
      );
    }
    const occupiedFiles = new Set([
      ...expectedManifest.images.map((row) => row.file),
      ...expectedManifest.imageAtomCases.map((row) => row.file),
    ]);
    const pathFiles = currentManifest.pathAtomCases.map((row) => row.file);
    if (
      new Set(pathFiles).size !== 3 ||
      pathFiles.some((file) => occupiedFiles.has(file))
    ) {
      throw new Error("PATH-ATOM append filenames collide or are not unique");
    }
    for (const renderCase of result.pathAtom.cases) {
      for (const image of renderCase.images) {
        const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
        fs.writeFileSync(path.join(goldenDir, image.file), png);
      }
    }
    expectedManifest = {
      ...expectedManifest,
      shaderDigests: currentManifest.shaderDigests,
      productionInputs: currentManifest.productionInputs,
      imageAtomInputs: currentManifest.imageAtomInputs,
      pathAtomInputs: currentManifest.pathAtomInputs,
      pathAtomCases: currentManifest.pathAtomCases,
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
    imageComparison = compareGoldenRows(
      currentManifest.images,
      expectedManifest.images,
    );
    imagePass = comparisonPass(imageComparison, expectedManifest.images, 21);
  }

  const imageAtomComparisonBeforeConnectorAppend = compareGoldenRows(
    currentManifest.imageAtomCases,
    expectedManifest?.imageAtomCases,
  );
  const imageAtomPassBeforeConnectorAppend = comparisonPass(
    imageAtomComparisonBeforeConnectorAppend,
    expectedManifest?.imageAtomCases,
    21,
  );
  const pathAtomComparisonBeforeConnectorAppend = compareGoldenRows(
    currentManifest.pathAtomCases,
    expectedManifest?.pathAtomCases,
  );
  const pathAtomPassBeforeConnectorAppend = comparisonPass(
    pathAtomComparisonBeforeConnectorAppend,
    expectedManifest?.pathAtomCases,
    3,
  );
  const connectorAppendPreflight = {
    bankPresent,
    legacyGoldensPass: imagePass,
    imageGoldensPass: imageAtomPassBeforeConnectorAppend,
    pathGoldensPass: pathAtomPassBeforeConnectorAppend,
    environmentMatch,
    legacyDeterminismPass: deterministic.length === 21 && determinismPass,
    imageDeterminismPass,
    pathDeterminismPass,
    connectorRows: currentManifest.connectorAtomCases.length,
    connectorDeterminismPass,
    connectorParityPass,
    connectorColorPass: result.connectorAtom.color?.pass,
    connectorArrangementPass: result.connectorAtom.arrangement?.pass,
    connectorLabelRoadPass: result.connectorAtom.labelRoad?.pass,
    connectorDarkLanePass: result.connectorAtom.darkLane?.pass,
    connectorUploadGatePass: result.connectorAtom.uploadGate?.pass,
    connectorContractPass: result.connectorAtom.pass,
    existingConnectorRows:
      expectedManifest?.connectorAtomCases?.length || 0,
  };
  const connectorAppendAuthorized =
    bankPresent &&
    imagePass &&
    imageAtomPassBeforeConnectorAppend &&
    pathAtomPassBeforeConnectorAppend &&
    environmentMatch &&
    deterministic.length === 21 &&
    determinismPass &&
    imageDeterminismPass &&
    pathDeterminismPass &&
    !expectedManifest.connectorAtomCases &&
    currentManifest.connectorAtomCases.length === 3 &&
    connectorDeterminismPass &&
    connectorParityPass &&
    result.connectorAtom.color?.pass &&
    result.connectorAtom.arrangement?.pass &&
    result.connectorAtom.labelRoad?.pass &&
    result.connectorAtom.darkLane?.pass &&
    result.connectorAtom.uploadGate?.pass &&
    result.connectorAtom.pass;

  if (appendConnectorGoldens) {
    if (!connectorAppendAuthorized) {
      throw new Error(
        `CONNECTOR-ATOM append preflight failed: ${JSON.stringify(connectorAppendPreflight)}`,
      );
    }
    const occupiedFiles = new Set([
      ...expectedManifest.images.map((row) => row.file),
      ...expectedManifest.imageAtomCases.map((row) => row.file),
      ...expectedManifest.pathAtomCases.map((row) => row.file),
    ]);
    const connectorFiles = currentManifest.connectorAtomCases.map(
      (row) => row.file,
    );
    if (
      new Set(connectorFiles).size !== 3 ||
      connectorFiles.some((file) => occupiedFiles.has(file))
    ) {
      throw new Error(
        "CONNECTOR-ATOM append filenames collide or are not unique",
      );
    }
    for (const renderCase of result.connectorAtom.cases) {
      for (const image of renderCase.images) {
        const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
        fs.writeFileSync(path.join(goldenDir, image.file), png);
      }
    }
    const imageInputAmendment = {
      amendment: (expectedManifest.imageAtomInputAmendments?.length || 0) + 1,
      reason:
        "CONNECTOR-ATOM source integration changed shared scene and verifier inputs with all prior image goldens byte-identical",
      inputs: currentManifest.imageAtomInputs,
    };
    const pathInputAmendment = {
      amendment: (expectedManifest.pathAtomInputAmendments?.length || 0) + 1,
      reason:
        "CONNECTOR-ATOM source integration changed shared scene and verifier inputs with all prior path goldens byte-identical",
      inputs: currentManifest.pathAtomInputs,
    };
    expectedManifest = {
      ...expectedManifest,
      shaderDigests: currentManifest.shaderDigests,
      productionInputs: currentManifest.productionInputs,
      imageAtomInputAmendments: [
        ...(expectedManifest.imageAtomInputAmendments || []),
        imageInputAmendment,
      ],
      pathAtomInputAmendments: [
        ...(expectedManifest.pathAtomInputAmendments || []),
        pathInputAmendment,
      ],
      connectorAtomInputs: currentManifest.connectorAtomInputs,
      connectorAtomCases: currentManifest.connectorAtomCases,
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
    imageComparison = compareGoldenRows(
      currentManifest.images,
      expectedManifest.images,
    );
    imagePass = comparisonPass(imageComparison, expectedManifest.images, 21);
  }

  const chromeHybridPass = Boolean(
    result.chromeAtom.hybridMetric?.metricPixelsEqual &&
      result.chromeAtom.hybridMetric?.anchorTracksContent &&
      result.chromeAtom.hybridMetric?.worldContentScales8x,
  );
  const chromeAppendPreflight = {
    bankPresent,
    legacyGoldensPass: imagePass,
    imageGoldensPass: imageAtomPassBeforeConnectorAppend,
    pathGoldensPass: pathAtomPassBeforeConnectorAppend,
    connectorGoldensPass: comparisonPass(
      compareGoldenRows(
        currentManifest.connectorAtomCases,
        expectedManifest?.connectorAtomCases,
      ),
      expectedManifest?.connectorAtomCases,
      3,
    ),
    environmentMatch,
    legacyDeterminismPass: deterministic.length === 21 && determinismPass,
    imageDeterminismPass,
    pathDeterminismPass,
    connectorDeterminismPass,
    existingChromeRows: expectedManifest?.chromeAtomCases?.length || 0,
    chromeRows: currentManifest.chromeAtomCases.length,
    chromeDeterminismPass,
    chromeHybridPass,
    chromeColorPass: result.chromeAtom.color?.pass,
    chromeArrangementPass: result.chromeAtom.arrangement?.pass,
    chromeUploadGatePass: result.chromeAtom.uploadGate?.pass,
    chromeResourcesPass: result.chromeAtom.resources?.pass,
    chromeStaticAbsencePass: chromeAbsence.pass,
    chromeContractPass: result.chromeAtom.pass,
  };
  const chromeAppendAuthorized =
    bankPresent &&
    imagePass &&
    imageAtomPassBeforeConnectorAppend &&
    pathAtomPassBeforeConnectorAppend &&
    chromeAppendPreflight.connectorGoldensPass &&
    environmentMatch &&
    deterministic.length === 21 &&
    determinismPass &&
    imageDeterminismPass &&
    pathDeterminismPass &&
    connectorDeterminismPass &&
    !expectedManifest.chromeAtomCases &&
    currentManifest.chromeAtomCases.length === 3 &&
    chromeDeterminismPass &&
    chromeHybridPass &&
    result.chromeAtom.color?.pass &&
    result.chromeAtom.arrangement?.pass &&
    result.chromeAtom.uploadGate?.pass &&
    result.chromeAtom.resources?.pass &&
    chromeAbsence.pass &&
    result.chromeAtom.pass;

  if (appendChromeGoldens) {
    if (!chromeAppendAuthorized) {
      throw new Error(
        `CHROME-ATOM append preflight failed: ${JSON.stringify(chromeAppendPreflight)}`,
      );
    }
    const occupiedFiles = new Set([
      ...expectedManifest.images.map((row) => row.file),
      ...expectedManifest.imageAtomCases.map((row) => row.file),
      ...expectedManifest.pathAtomCases.map((row) => row.file),
      ...expectedManifest.connectorAtomCases.map((row) => row.file),
    ]);
    const occupiedDigestsBefore = Object.fromEntries(
      [...occupiedFiles].map((file) => [
        file,
        sha256(fs.readFileSync(path.join(goldenDir, file))),
      ]),
    );
    const chromeFiles = currentManifest.chromeAtomCases.map((row) => row.file);
    if (
      new Set(chromeFiles).size !== 3 ||
      chromeFiles.some((file) => occupiedFiles.has(file))
    ) {
      throw new Error("CHROME-ATOM append filenames collide or are not unique");
    }
    for (const renderCase of result.chromeAtom.cases) {
      for (const image of renderCase.images) {
        const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
        fs.writeFileSync(path.join(goldenDir, image.file), png);
      }
    }
    const priorGoldenBytesUnchanged = Object.entries(
      occupiedDigestsBefore,
    ).every(
      ([file, digest]) =>
        sha256(fs.readFileSync(path.join(goldenDir, file))) === digest,
    );
    if (!priorGoldenBytesUnchanged) {
      throw new Error("CHROME-ATOM append changed an existing golden byte");
    }
    const imageInputAmendment = {
      amendment: (expectedManifest.imageAtomInputAmendments?.length || 0) + 1,
      reason:
        "CHROME-ATOM source integration changed shared scene and verifier inputs with all prior image goldens byte-identical",
      inputs: currentManifest.imageAtomInputs,
    };
    const pathInputAmendment = {
      amendment: (expectedManifest.pathAtomInputAmendments?.length || 0) + 1,
      reason:
        "CHROME-ATOM source integration changed shared scene, live-atom, renderer, and verifier inputs with all prior path goldens byte-identical",
      inputs: currentManifest.pathAtomInputs,
    };
    const connectorInputAmendment = {
      amendment:
        (expectedManifest.connectorAtomInputAmendments?.length || 0) + 1,
      reason:
        "CHROME-ATOM source integration changed shared scene, live-atom, renderer, and verifier inputs with all prior connector goldens byte-identical",
      inputs: currentManifest.connectorAtomInputs,
    };
    expectedManifest = {
      ...expectedManifest,
      shaderDigests: currentManifest.shaderDigests,
      productionInputs: currentManifest.productionInputs,
      imageAtomInputAmendments: [
        ...(expectedManifest.imageAtomInputAmendments || []),
        imageInputAmendment,
      ],
      pathAtomInputAmendments: [
        ...(expectedManifest.pathAtomInputAmendments || []),
        pathInputAmendment,
      ],
      connectorAtomInputAmendments: [
        ...(expectedManifest.connectorAtomInputAmendments || []),
        connectorInputAmendment,
      ],
      chromeAtomInputs: currentManifest.chromeAtomInputs,
      chromeAtomCases: currentManifest.chromeAtomCases,
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
    imageComparison = compareGoldenRows(
      currentManifest.images,
      expectedManifest.images,
    );
    imagePass = comparisonPass(imageComparison, expectedManifest.images, 21);
    chromeAppendPreflight.priorGoldenBytesUnchanged = true;
  }

  const priorGoldenSetsPass =
    imagePass &&
    comparisonPass(
      compareGoldenRows(
        currentManifest.imageAtomCases,
        expectedManifest?.imageAtomCases,
      ),
      expectedManifest?.imageAtomCases,
      21,
    ) &&
    comparisonPass(
      compareGoldenRows(
        currentManifest.pathAtomCases,
        expectedManifest?.pathAtomCases,
      ),
      expectedManifest?.pathAtomCases,
      3,
    ) &&
    comparisonPass(
      compareGoldenRows(
        currentManifest.connectorAtomCases,
        expectedManifest?.connectorAtomCases,
      ),
      expectedManifest?.connectorAtomCases,
      3,
    ) &&
    comparisonPass(
      compareGoldenRows(
        currentManifest.chromeAtomCases,
        expectedManifest?.chromeAtomCases,
      ),
      expectedManifest?.chromeAtomCases,
      3,
    );
  const w4AppendPreflight = {
    bankPresent,
    environmentMatch,
    priorGoldenSetsPass,
    existingW4Rows: expectedManifest?.w4FrameRuntimeCases?.length || 0,
    w4Rows: currentManifest.w4FrameRuntimeCases.length,
    w4DeterminismPass,
    w4ContractPass: result.w4FrameRuntime.pass,
  };
  const w4AppendAuthorized =
    bankPresent &&
    environmentMatch &&
    priorGoldenSetsPass &&
    !expectedManifest.w4FrameRuntimeCases &&
    currentManifest.w4FrameRuntimeCases.length === 3 &&
    w4DeterminismPass &&
    result.w4FrameRuntime.pass;

  if (appendW4Goldens) {
    if (!w4AppendAuthorized) {
      throw new Error(
        `W4 FRAME-RUNTIME append preflight failed: ${JSON.stringify(w4AppendPreflight)}`,
      );
    }
    const occupiedFiles = new Set([
      ...expectedManifest.images.map((row) => row.file),
      ...expectedManifest.imageAtomCases.map((row) => row.file),
      ...expectedManifest.pathAtomCases.map((row) => row.file),
      ...expectedManifest.connectorAtomCases.map((row) => row.file),
      ...expectedManifest.chromeAtomCases.map((row) => row.file),
    ]);
    const occupiedDigests = Object.fromEntries(
      [...occupiedFiles].map((file) => [
        file,
        sha256(fs.readFileSync(path.join(goldenDir, file))),
      ]),
    );
    const w4Files = currentManifest.w4FrameRuntimeCases.map((row) => row.file);
    if (
      new Set(w4Files).size !== 3 ||
      w4Files.some((file) => occupiedFiles.has(file))
    ) {
      throw new Error("W4 FRAME-RUNTIME filenames collide or are not unique");
    }
    for (const renderCase of result.w4FrameRuntime.cases) {
      for (const image of renderCase.images) {
        const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
        fs.writeFileSync(path.join(goldenDir, image.file), png);
      }
    }
    if (
      !Object.entries(occupiedDigests).every(
        ([file, digest]) =>
          sha256(fs.readFileSync(path.join(goldenDir, file))) === digest,
      )
    ) {
      throw new Error("W4 FRAME-RUNTIME append changed an existing golden byte");
    }
    expectedManifest = {
      ...expectedManifest,
      w4FrameRuntimeInputs: currentManifest.w4FrameRuntimeInputs,
      w4FrameRuntimeCases: currentManifest.w4FrameRuntimeCases,
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
    w4AppendPreflight.priorGoldenBytesUnchanged = true;
  }

  const priorT2GoldenSetsPass =
    priorGoldenSetsPass &&
    comparisonPass(
      compareGoldenRows(
        currentManifest.w4FrameRuntimeCases,
        expectedManifest?.w4FrameRuntimeCases,
      ),
      expectedManifest?.w4FrameRuntimeCases,
      3,
    );
  const priorInputAmendmentsRecorded = [
    ["imageAtomInputAmendments", "imageAtomInputs"],
    ["pathAtomInputAmendments", "pathAtomInputs"],
    ["connectorAtomInputAmendments", "connectorAtomInputs"],
    ["chromeAtomInputAmendments", "chromeAtomInputs"],
    ["w4FrameRuntimeInputAmendments", "w4FrameRuntimeInputs"],
  ].every(([amendmentKey, inputKey]) => {
    const expectedInputs =
      expectedManifest[amendmentKey]?.at(-1)?.inputs ||
      expectedManifest[inputKey];
    return (
      Boolean(expectedInputs) &&
      fingerprintSha(expectedInputs) ===
      fingerprintSha(currentManifest[inputKey])
    );
  });
  let chromeNamespaceCustodyRows = Object.keys(chromeSourceFiles).map(
    (key) => ({
      key,
      expected:
        (expectedManifest.chromeAtomInputAmendments?.at(-1)?.inputs ||
          expectedManifest.chromeAtomInputs)?.[key]?.sha256 || null,
      current: currentManifest.chromeAtomInputs?.[key]?.sha256 || null,
      match:
        (expectedManifest.chromeAtomInputAmendments?.at(-1)?.inputs ||
          expectedManifest.chromeAtomInputs)?.[key]?.sha256 ===
        currentManifest.chromeAtomInputs?.[key]?.sha256,
    }),
  );
  let chromeNamespaceCustodyPass =
    chromeNamespaceCustodyRows.length === 6 &&
    chromeNamespaceCustodyRows.every((row) => row.match);
  let pickRoadCustodyRows = [
    "imageAtomInputs",
    "pathAtomInputs",
    "connectorAtomInputs",
    "chromeAtomInputs",
    "w4FrameRuntimeInputs",
  ].flatMap((inputKey) =>
    ["sceneStore", "rectTree"].map((key) => ({
      inputKey,
      key,
      expected:
        ({
          imageAtomInputs: expectedManifest.imageAtomInputAmendments,
          pathAtomInputs: expectedManifest.pathAtomInputAmendments,
          connectorAtomInputs: expectedManifest.connectorAtomInputAmendments,
          chromeAtomInputs: expectedManifest.chromeAtomInputAmendments,
          w4FrameRuntimeInputs:
            expectedManifest.w4FrameRuntimeInputAmendments,
        }[inputKey]?.at(-1)?.inputs || expectedManifest[inputKey])?.[key]
          ?.sha256 || null,
      current: currentManifest[inputKey]?.[key]?.sha256 || null,
      match:
        ({
          imageAtomInputs: expectedManifest.imageAtomInputAmendments,
          pathAtomInputs: expectedManifest.pathAtomInputAmendments,
          connectorAtomInputs: expectedManifest.connectorAtomInputAmendments,
          chromeAtomInputs: expectedManifest.chromeAtomInputAmendments,
          w4FrameRuntimeInputs:
            expectedManifest.w4FrameRuntimeInputAmendments,
        }[inputKey]?.at(-1)?.inputs || expectedManifest[inputKey])?.[key]
          ?.sha256 ===
        currentManifest[inputKey]?.[key]?.sha256,
    })),
  );
  let pickRoadCustodyPass =
    pickRoadCustodyRows.length === 10 &&
    pickRoadCustodyRows.every((row) => row.match);
  const t2AppendPreflight = {
    bankPresent,
    environmentMatch,
    priorGoldenSetsPass: priorT2GoldenSetsPass,
    priorInputAmendmentsRecorded,
    chromeNamespaceCustodyPass,
    pickRoadCustodyPass,
    t2StaticAbsencePass: t2Absence.pass,
    existingT2Rows: expectedManifest?.t2InputFloorCases?.length || 0,
    t2Rows: currentManifest.t2InputFloorCases.length,
    t2DeterminismPass,
    t2ContractPass: result.t2InputFloor.pass,
  };
  const t2AppendAuthorized =
    bankPresent &&
    environmentMatch &&
    priorT2GoldenSetsPass &&
    priorInputAmendmentsRecorded &&
    chromeNamespaceCustodyPass &&
    pickRoadCustodyPass &&
    t2Absence.pass &&
    !expectedManifest.t2InputFloorCases &&
    currentManifest.t2InputFloorCases.length === 3 &&
    t2DeterminismPass &&
    result.t2InputFloor.pass;

  if (appendT2Goldens) {
    if (!t2AppendAuthorized) {
      throw new Error(
        `T2 INPUT-FLOOR append preflight failed: ${JSON.stringify(t2AppendPreflight)}`,
      );
    }
    const occupiedFiles = new Set([
      ...expectedManifest.images.map((row) => row.file),
      ...expectedManifest.imageAtomCases.map((row) => row.file),
      ...expectedManifest.pathAtomCases.map((row) => row.file),
      ...expectedManifest.connectorAtomCases.map((row) => row.file),
      ...expectedManifest.chromeAtomCases.map((row) => row.file),
      ...expectedManifest.w4FrameRuntimeCases.map((row) => row.file),
    ]);
    const occupiedDigests = Object.fromEntries(
      [...occupiedFiles].map((file) => [
        file,
        sha256(fs.readFileSync(path.join(goldenDir, file))),
      ]),
    );
    const t2Files = currentManifest.t2InputFloorCases.map((row) => row.file);
    if (
      new Set(t2Files).size !== 3 ||
      t2Files.some((file) => occupiedFiles.has(file))
    ) {
      throw new Error("T2 INPUT-FLOOR filenames collide or are not unique");
    }
    for (const renderCase of result.t2InputFloor.cases) {
      for (const image of renderCase.images) {
        const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
        fs.writeFileSync(path.join(goldenDir, image.file), png);
      }
    }
    if (
      !Object.entries(occupiedDigests).every(
        ([file, digest]) =>
          sha256(fs.readFileSync(path.join(goldenDir, file))) === digest,
      )
    ) {
      throw new Error("T2 INPUT-FLOOR append changed an existing golden byte");
    }
    expectedManifest = {
      ...expectedManifest,
      t2InputFloorInputs: currentManifest.t2InputFloorInputs,
      t2InputFloorCases: currentManifest.t2InputFloorCases,
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
    t2AppendPreflight.priorGoldenBytesUnchanged = true;
  }

  const priorRegion3dGoldenSetsPass =
    priorT2GoldenSetsPass &&
    comparisonPass(
      compareGoldenRows(
        currentManifest.t2InputFloorCases,
        expectedManifest?.t2InputFloorCases,
      ),
      expectedManifest?.t2InputFloorCases,
      3,
    );
  const region3dAppendPreflight = {
    bankPresent,
    environmentMatch,
    priorGoldenSetsPass: priorRegion3dGoldenSetsPass,
    existingRegion3dRows: expectedManifest?.region3dFloorCases?.length || 0,
    region3dRows: currentManifest.region3dFloorCases.length,
    region3dDeterminismPass,
    s1Pass: result.region3dFloor.s1?.pass,
    s2Pass: result.region3dFloor.s2?.pass,
    s3Pass: result.region3dFloor.s3?.pass,
    s4Pass: result.region3dFloor.s4?.pass,
    s5Pass: result.region3dFloor.s5?.pass,
    region3dContractPass: result.region3dFloor.pass,
  };
  const region3dAppendAuthorized =
    bankPresent &&
    environmentMatch &&
    priorRegion3dGoldenSetsPass &&
    !expectedManifest.region3dFloorCases &&
    currentManifest.region3dFloorCases.length === 3 &&
    region3dDeterminismPass &&
    result.region3dFloor.s1?.pass &&
    result.region3dFloor.s2?.pass &&
    result.region3dFloor.s3?.pass &&
    result.region3dFloor.s4?.pass &&
    result.region3dFloor.s5?.pass &&
    result.region3dFloor.pass;

  if (appendRegion3dGoldens) {
    if (!region3dAppendAuthorized) {
      throw new Error(
        `REGION3D-FLOOR append preflight failed: ${JSON.stringify(region3dAppendPreflight)}`,
      );
    }
    const priorGoldenRows = [
      ...expectedManifest.images,
      ...expectedManifest.imageAtomCases,
      ...expectedManifest.pathAtomCases,
      ...expectedManifest.connectorAtomCases,
      ...expectedManifest.chromeAtomCases,
      ...expectedManifest.w4FrameRuntimeCases,
      ...expectedManifest.t2InputFloorCases,
    ];
    const occupiedFiles = new Set(priorGoldenRows.map((row) => row.file));
    const occupiedDigests = Object.fromEntries(
      [...occupiedFiles].map((file) => [
        file,
        sha256(fs.readFileSync(path.join(goldenDir, file))),
      ]),
    );
    const region3dFiles = currentManifest.region3dFloorCases.map(
      (row) => row.file,
    );
    if (
      new Set(region3dFiles).size !== 3 ||
      region3dFiles.some((file) => occupiedFiles.has(file))
    ) {
      throw new Error("REGION3D-FLOOR filenames collide or are not unique");
    }
    for (const renderCase of result.region3dFloor.cases) {
      for (const image of renderCase.images) {
        const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
        fs.writeFileSync(path.join(goldenDir, image.file), png);
      }
    }
    if (
      !Object.entries(occupiedDigests).every(
        ([file, digest]) =>
          sha256(fs.readFileSync(path.join(goldenDir, file))) === digest,
      )
    ) {
      throw new Error("REGION3D-FLOOR append changed an existing golden byte");
    }
    const amendmentFor = (key, inputs) => ({
      amendment: (expectedManifest[key]?.length || 0) + 1,
      reason:
        "REGION3D-FLOOR integrated shared render/store seams; every prior golden remained byte-identical under the scoped append road",
      inputs,
    });
    expectedManifest = {
      ...expectedManifest,
      shaderDigests: currentManifest.shaderDigests,
      productionInputs: currentManifest.productionInputs,
      imageAtomInputAmendments: [
        ...(expectedManifest.imageAtomInputAmendments || []),
        amendmentFor("imageAtomInputAmendments", currentManifest.imageAtomInputs),
      ],
      pathAtomInputAmendments: [
        ...(expectedManifest.pathAtomInputAmendments || []),
        amendmentFor("pathAtomInputAmendments", currentManifest.pathAtomInputs),
      ],
      connectorAtomInputAmendments: [
        ...(expectedManifest.connectorAtomInputAmendments || []),
        amendmentFor(
          "connectorAtomInputAmendments",
          currentManifest.connectorAtomInputs,
        ),
      ],
      chromeAtomInputAmendments: [
        ...(expectedManifest.chromeAtomInputAmendments || []),
        amendmentFor("chromeAtomInputAmendments", currentManifest.chromeAtomInputs),
      ],
      w4FrameRuntimeInputAmendments: [
        ...(expectedManifest.w4FrameRuntimeInputAmendments || []),
        amendmentFor(
          "w4FrameRuntimeInputAmendments",
          currentManifest.w4FrameRuntimeInputs,
        ),
      ],
      t2InputFloorInputAmendments: [
        ...(expectedManifest.t2InputFloorInputAmendments || []),
        amendmentFor(
          "t2InputFloorInputAmendments",
          currentManifest.t2InputFloorInputs,
        ),
      ],
      region3dFloorInputs: currentManifest.region3dFloorInputs,
      region3dFloorCases: currentManifest.region3dFloorCases,
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
    chromeNamespaceCustodyRows = Object.keys(chromeSourceFiles).map((key) => ({
      key,
      expected: currentManifest.chromeAtomInputs[key]?.sha256 || null,
      current: currentManifest.chromeAtomInputs[key]?.sha256 || null,
      match: true,
    }));
    chromeNamespaceCustodyPass = chromeNamespaceCustodyRows.length === 6;
    pickRoadCustodyRows = [
      "imageAtomInputs",
      "pathAtomInputs",
      "connectorAtomInputs",
      "chromeAtomInputs",
      "w4FrameRuntimeInputs",
    ].flatMap((inputKey) =>
      ["sceneStore", "rectTree"].map((key) => ({
        inputKey,
        key,
        expected: currentManifest[inputKey]?.[key]?.sha256 || null,
        current: currentManifest[inputKey]?.[key]?.sha256 || null,
        match: true,
      })),
    );
    pickRoadCustodyPass = pickRoadCustodyRows.length === 10;
    region3dAppendPreflight.priorGoldenBytesUnchanged = true;
  }

  const latestInputs = (baseKey, amendmentKey) =>
    expectedManifest[amendmentKey]?.at(-1)?.inputs ||
    expectedManifest[baseKey];
  const changedInputKeys = (baseKey, amendmentKey) => {
    const expected = latestInputs(baseKey, amendmentKey);
    const current = currentManifest[baseKey];
    return Object.keys(current).filter(
      (key) => expected?.[key]?.sha256 !== current[key]?.sha256,
    );
  };
  const t2LivedCorrectionInputDebt = {
    imageAtomInputs: changedInputKeys(
      "imageAtomInputs",
      "imageAtomInputAmendments",
    ),
    pathAtomInputs: changedInputKeys(
      "pathAtomInputs",
      "pathAtomInputAmendments",
    ),
    connectorAtomInputs: changedInputKeys(
      "connectorAtomInputs",
      "connectorAtomInputAmendments",
    ),
    chromeAtomInputs: changedInputKeys(
      "chromeAtomInputs",
      "chromeAtomInputAmendments",
    ),
    w4FrameRuntimeInputs: changedInputKeys(
      "w4FrameRuntimeInputs",
      "w4FrameRuntimeInputAmendments",
    ),
    t2InputFloorInputs: changedInputKeys(
      "t2InputFloorInputs",
      "t2InputFloorInputAmendments",
    ),
  };
  const expectedT2LivedCorrectionInputDebt = {
    imageAtomInputs: ["verifier"],
    pathAtomInputs: ["renderer", "verifier"],
    connectorAtomInputs: ["renderer", "verifier"],
    chromeAtomInputs: ["renderer", "verifier", "runner"],
    w4FrameRuntimeInputs: ["renderer", "verifier", "runner"],
    t2InputFloorInputs: ["editingRuntime", "renderer", "verifier", "runner"],
  };
  const t2LivedCorrectionRows = compareGoldenRows(
    currentManifest.t2InputFloorCases,
    expectedManifest?.t2InputFloorCases,
  );
  const t2LivedCorrectionChangedRows = t2LivedCorrectionRows.filter(
    (row) => !row.rawMatch || !row.pngManifestMatch,
  );
  const t2LivedCorrectionProductionDebt = Object.keys(
    currentManifest.productionInputs,
  ).filter(
    (key) =>
      expectedManifest.productionInputs?.[key]?.sha256 !==
      currentManifest.productionInputs[key]?.sha256,
  );
  const t2LivedCorrectionPreflight = {
    bankPresent,
    environmentMatch,
    priorGoldenSetsPass: priorT2GoldenSetsPass,
    productionDebt: t2LivedCorrectionProductionDebt,
    inputDebt: t2LivedCorrectionInputDebt,
    changedT2Cases: t2LivedCorrectionChangedRows.map((row) => row.caseId),
    unchangedT2CasesPass: t2LivedCorrectionRows
      .filter((row) => row.caseId !== "paragraph-selection")
      .every(
        (row) => row.rawMatch && row.pngManifestMatch && row.goldenFileMatch,
      ),
    priorParagraphGoldenPresent:
      t2LivedCorrectionChangedRows[0]?.goldenFileMatch || false,
    t2DeterminismPass,
    t2ContractPass: result.t2InputFloor.pass,
    clipPass: result.t2InputFloor.clip?.pass,
  };
  const t2LivedCorrectionAuthorized =
    bankPresent &&
    environmentMatch &&
    priorT2GoldenSetsPass &&
    JSON.stringify(t2LivedCorrectionProductionDebt) ===
      JSON.stringify(["rendererSource"]) &&
    JSON.stringify(t2LivedCorrectionInputDebt) ===
      JSON.stringify(expectedT2LivedCorrectionInputDebt) &&
    t2LivedCorrectionChangedRows.length === 1 &&
    t2LivedCorrectionChangedRows[0].caseId === "paragraph-selection" &&
    t2LivedCorrectionChangedRows[0].goldenFileMatch &&
    t2LivedCorrectionPreflight.unchangedT2CasesPass &&
    t2DeterminismPass &&
    result.t2InputFloor.clip?.pass &&
    result.t2InputFloor.pass;

  if (amendT2LivedCorrection) {
    if (!t2LivedCorrectionAuthorized) {
      throw new Error(
        `T2 lived-correction amendment preflight failed: ${JSON.stringify(t2LivedCorrectionPreflight)}`,
      );
    }
    const paragraphCase = result.t2InputFloor.cases.find(
      (renderCase) => renderCase.caseId === "paragraph-selection",
    );
    const paragraphImage = paragraphCase?.images?.[0];
    if (!paragraphImage) {
      throw new Error("T2 lived correction lacks paragraph-selection pixels");
    }
    const protectedGoldenRows = [
      ...expectedManifest.images,
      ...expectedManifest.imageAtomCases,
      ...expectedManifest.pathAtomCases,
      ...expectedManifest.connectorAtomCases,
      ...expectedManifest.chromeAtomCases,
      ...expectedManifest.w4FrameRuntimeCases,
      ...expectedManifest.t2InputFloorCases.filter(
        (row) => row.caseId !== "paragraph-selection",
      ),
    ];
    const protectedGoldenDigests = new Map(
      protectedGoldenRows.map((row) => [
        row.file,
        sha256(fs.readFileSync(path.join(goldenDir, row.file))),
      ]),
    );
    const png = Buffer.from(paragraphImage.pngDataUrl.split(",", 2)[1], "base64");
    fs.writeFileSync(path.join(goldenDir, paragraphImage.file), png);
    if (
      [...protectedGoldenDigests].some(
        ([file, digest]) =>
          sha256(fs.readFileSync(path.join(goldenDir, file))) !== digest,
      )
    ) {
      throw new Error("T2 lived correction changed an unrelated golden byte");
    }
    const amendmentFor = (key, inputs) => ({
      amendment: (expectedManifest[key]?.length || 0) + 1,
      reason:
        "T2 lived correction repaired paragraph containment and device-pixel clip projection; unrelated golden bytes remain unchanged",
      inputs,
    });
    const priorParagraph = expectedManifest.t2InputFloorCases.find(
      (row) => row.caseId === "paragraph-selection",
    );
    const currentParagraph = currentManifest.t2InputFloorCases.find(
      (row) => row.caseId === "paragraph-selection",
    );
    expectedManifest = {
      ...expectedManifest,
      productionInputs: currentManifest.productionInputs,
      imageAtomInputAmendments: [
        ...(expectedManifest.imageAtomInputAmendments || []),
        amendmentFor("imageAtomInputAmendments", currentManifest.imageAtomInputs),
      ],
      pathAtomInputAmendments: [
        ...(expectedManifest.pathAtomInputAmendments || []),
        amendmentFor("pathAtomInputAmendments", currentManifest.pathAtomInputs),
      ],
      connectorAtomInputAmendments: [
        ...(expectedManifest.connectorAtomInputAmendments || []),
        amendmentFor(
          "connectorAtomInputAmendments",
          currentManifest.connectorAtomInputs,
        ),
      ],
      chromeAtomInputAmendments: [
        ...(expectedManifest.chromeAtomInputAmendments || []),
        amendmentFor("chromeAtomInputAmendments", currentManifest.chromeAtomInputs),
      ],
      w4FrameRuntimeInputAmendments: [
        ...(expectedManifest.w4FrameRuntimeInputAmendments || []),
        amendmentFor(
          "w4FrameRuntimeInputAmendments",
          currentManifest.w4FrameRuntimeInputs,
        ),
      ],
      t2InputFloorInputAmendments: [
        ...(expectedManifest.t2InputFloorInputAmendments || []),
        amendmentFor(
          "t2InputFloorInputAmendments",
          currentManifest.t2InputFloorInputs,
        ),
      ],
      t2InputFloorGoldenAmendments: [
        ...(expectedManifest.t2InputFloorGoldenAmendments || []),
        {
          amendment:
            (expectedManifest.t2InputFloorGoldenAmendments?.length || 0) + 1,
          reason:
            "Lived paragraph text escaped its fixed background; freeze the corrected layout-owned block height",
          before: priorParagraph,
          after: currentParagraph,
        },
      ],
      t2InputFloorCases: currentManifest.t2InputFloorCases,
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
    t2LivedCorrectionPreflight.unrelatedGoldenBytesUnchanged = true;
  }

  const changedShaderDigestKeys = Object.keys(currentManifest.shaderDigests)
    .filter(
      (key) =>
        expectedManifest?.shaderDigests?.[key] !==
        currentManifest.shaderDigests[key],
    )
    .sort();
  const shaderAmendmentW4Pass = comparisonPass(
    compareGoldenRows(
      currentManifest.w4FrameRuntimeCases,
      expectedManifest?.w4FrameRuntimeCases,
    ),
    expectedManifest?.w4FrameRuntimeCases,
    3,
  );
  const shaderAmendmentPreflight = {
    bankPresent,
    environmentMatch,
    priorGoldenSetsPass,
    w4GoldensPass: shaderAmendmentW4Pass,
    changedShaderDigestKeys,
    productionInputDebtKeys,
    w4ContractPass: result.w4FrameRuntime.pass,
  };
  const shaderAmendmentAuthorized =
    bankPresent &&
    environmentMatch &&
    priorGoldenSetsPass &&
    shaderAmendmentW4Pass &&
    JSON.stringify(changedShaderDigestKeys) ===
      JSON.stringify(["rectFragment"]) &&
    JSON.stringify(productionInputDebtKeys) ===
      JSON.stringify(["rendererSource"]) &&
    result.w4FrameRuntime.gradient?.pass &&
    result.w4FrameRuntime.pass;

  if (amendShaderDigests) {
    if (!shaderAmendmentAuthorized) {
      throw new Error(
        `W4 shader-digest amendment preflight failed: ${JSON.stringify(shaderAmendmentPreflight)}`,
      );
    }
    expectedManifest = {
      ...expectedManifest,
      shaderDigests: currentManifest.shaderDigests,
      productionInputs: currentManifest.productionInputs,
      shaderDigestAmendments: [
        ...(expectedManifest.shaderDigestAmendments || []),
        {
          amendment: (expectedManifest.shaderDigestAmendments?.length || 0) + 1,
          reason:
            "W4 decodes rect gradient stops before linear interpolation; every prior and W4 golden remained byte-stable under its declared road",
          changedKeys: changedShaderDigestKeys,
          shaderDigests: currentManifest.shaderDigests,
          productionInputs: currentManifest.productionInputs,
        },
      ],
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
  }

  const sourceMatch =
    bankPresent &&
    fingerprintSha({
      shaderDigests: expectedManifest.shaderDigests,
      productionInputs: expectedManifest.productionInputs,
    }) ===
      fingerprintSha({
        shaderDigests: currentManifest.shaderDigests,
        productionInputs: currentManifest.productionInputs,
      });
  let expectedImageAtomInputs =
    expectedManifest.imageAtomInputAmendments?.at(-1)?.inputs ||
    expectedManifest.imageAtomInputs;
  let imageAtomInputsMatch =
    bankPresent &&
    fingerprintSha(expectedImageAtomInputs) ===
      fingerprintSha(currentManifest.imageAtomInputs);
  const imageAtomComparison = compareGoldenRows(
    currentManifest.imageAtomCases,
    expectedManifest?.imageAtomCases,
  );
  const imageAtomPass =
    bankPresent &&
    comparisonPass(
      imageAtomComparison,
      expectedManifest?.imageAtomCases,
      21,
    );
  let expectedPathAtomInputs =
    expectedManifest.pathAtomInputAmendments?.at(-1)?.inputs ||
    expectedManifest.pathAtomInputs;
  let pathAtomInputsMatch =
    bankPresent &&
    fingerprintSha(expectedPathAtomInputs) ===
      fingerprintSha(currentManifest.pathAtomInputs);
  const pathAtomComparison = compareGoldenRows(
    currentManifest.pathAtomCases,
    expectedManifest?.pathAtomCases,
  );
  const pathAtomPass =
    bankPresent &&
    comparisonPass(
      pathAtomComparison,
      expectedManifest?.pathAtomCases,
      3,
    );
  let expectedConnectorAtomInputs =
    expectedManifest.connectorAtomInputAmendments?.at(-1)?.inputs ||
    expectedManifest.connectorAtomInputs;
  let connectorAtomInputsMatch =
    bankPresent &&
    Boolean(expectedConnectorAtomInputs) &&
    fingerprintSha(expectedConnectorAtomInputs) ===
      fingerprintSha(currentManifest.connectorAtomInputs);
  const connectorAtomComparison = compareGoldenRows(
    currentManifest.connectorAtomCases,
    expectedManifest?.connectorAtomCases,
  );
  const connectorAtomPass =
    bankPresent &&
    comparisonPass(
      connectorAtomComparison,
      expectedManifest?.connectorAtomCases,
      3,
    );
  let expectedChromeAtomInputs =
    expectedManifest.chromeAtomInputAmendments?.at(-1)?.inputs ||
    expectedManifest.chromeAtomInputs;
  let chromeAtomInputsMatch =
    bankPresent &&
    Boolean(expectedChromeAtomInputs) &&
    fingerprintSha(expectedChromeAtomInputs) ===
      fingerprintSha(currentManifest.chromeAtomInputs);
  const chromeAtomComparison = compareGoldenRows(
    currentManifest.chromeAtomCases,
    expectedManifest?.chromeAtomCases,
  );
  const chromeAtomPass =
    bankPresent &&
    comparisonPass(
      chromeAtomComparison,
      expectedManifest?.chromeAtomCases,
      3,
    );
  let expectedW4FrameRuntimeInputs =
    expectedManifest.w4FrameRuntimeInputAmendments?.at(-1)?.inputs ||
    expectedManifest.w4FrameRuntimeInputs;
  let w4FrameRuntimeInputsMatch =
    bankPresent &&
    Boolean(expectedW4FrameRuntimeInputs) &&
    fingerprintSha(expectedW4FrameRuntimeInputs) ===
      fingerprintSha(currentManifest.w4FrameRuntimeInputs);
  const w4FrameRuntimeComparison = compareGoldenRows(
    currentManifest.w4FrameRuntimeCases,
    expectedManifest?.w4FrameRuntimeCases,
  );
  const w4FrameRuntimePass =
    bankPresent &&
    comparisonPass(
      w4FrameRuntimeComparison,
      expectedManifest?.w4FrameRuntimeCases,
      3,
    );
  let expectedT2InputFloorInputs =
    expectedManifest.t2InputFloorInputAmendments?.at(-1)?.inputs ||
    expectedManifest.t2InputFloorInputs;
  let t2InputFloorInputsMatch =
    bankPresent &&
    Boolean(expectedT2InputFloorInputs) &&
    fingerprintSha(expectedT2InputFloorInputs) ===
      fingerprintSha(currentManifest.t2InputFloorInputs);
  const t2InputFloorComparison = compareGoldenRows(
    currentManifest.t2InputFloorCases,
    expectedManifest?.t2InputFloorCases,
  );
  const t2InputFloorPass =
    bankPresent &&
    comparisonPass(
      t2InputFloorComparison,
      expectedManifest?.t2InputFloorCases,
      3,
    );
  let expectedRegion3dFloorInputs =
    expectedManifest.region3dFloorInputAmendments?.at(-1)?.inputs ||
    expectedManifest.region3dFloorInputs;
  let region3dFloorInputsMatch =
    bankPresent &&
    Boolean(expectedRegion3dFloorInputs) &&
    fingerprintSha(expectedRegion3dFloorInputs) ===
      fingerprintSha(currentManifest.region3dFloorInputs);
  let region3dFloorComparison = compareGoldenRows(
    currentManifest.region3dFloorCases,
    expectedManifest?.region3dFloorCases,
  );
  let region3dFloorPass =
    bankPresent &&
    comparisonPass(
      region3dFloorComparison,
      expectedManifest?.region3dFloorCases,
      3,
    );
  const region3dGizmoChangedRows = region3dFloorComparison.filter(
    (row) => !row.rawMatch || !row.pngManifestMatch,
  );
  const region3dGizmoAmendmentPreflight = {
    bankPresent,
    sourceMatch,
    environmentMatch,
    priorGoldenSetsPass: priorRegion3dGoldenSetsPass,
    changedCases: region3dGizmoChangedRows.map((row) => row.caseId),
    changedGoldenPresent:
      region3dGizmoChangedRows.length === 1 &&
      region3dGizmoChangedRows[0].goldenFileMatch,
    unchangedRegionGoldensPass: region3dFloorComparison
      .filter((row) => row.caseId !== "gizmo-overlay")
      .every(
        (row) => row.rawMatch && row.pngManifestMatch && row.goldenFileMatch,
      ),
    region3dDeterminismPass,
    region3dContractPass: result.region3dFloor.pass,
  };
  const region3dGizmoAmendmentAuthorized =
    bankPresent &&
    sourceMatch &&
    environmentMatch &&
    priorRegion3dGoldenSetsPass &&
    region3dGizmoChangedRows.length === 1 &&
    region3dGizmoChangedRows[0].caseId === "gizmo-overlay" &&
    region3dGizmoChangedRows[0].goldenFileMatch &&
    region3dGizmoAmendmentPreflight.unchangedRegionGoldensPass &&
    region3dDeterminismPass &&
    result.region3dFloor.s1?.pass &&
    result.region3dFloor.s2?.pass &&
    result.region3dFloor.s3?.pass &&
    result.region3dFloor.s4?.pass &&
    result.region3dFloor.s5?.pass &&
    result.region3dFloor.pass;

  if (amendRegion3dGizmoGolden) {
    if (!region3dGizmoAmendmentAuthorized) {
      throw new Error(
        `REGION3D-FLOOR gizmo amendment preflight failed: ${JSON.stringify(region3dGizmoAmendmentPreflight)}`,
      );
    }
    const protectedRows = [
      ...expectedManifest.images,
      ...expectedManifest.imageAtomCases,
      ...expectedManifest.pathAtomCases,
      ...expectedManifest.connectorAtomCases,
      ...expectedManifest.chromeAtomCases,
      ...expectedManifest.w4FrameRuntimeCases,
      ...expectedManifest.t2InputFloorCases,
      ...expectedManifest.region3dFloorCases.filter(
        (row) => row.caseId !== "gizmo-overlay",
      ),
    ];
    const protectedDigests = new Map(
      protectedRows.map((row) => [
        row.file,
        sha256(fs.readFileSync(path.join(goldenDir, row.file))),
      ]),
    );
    const gizmoCase = result.region3dFloor.cases.find(
      (renderCase) => renderCase.caseId === "gizmo-overlay",
    );
    const gizmoImage = gizmoCase?.images?.[0];
    if (!gizmoImage) {
      throw new Error("REGION3D-FLOOR gizmo amendment lacks pixels");
    }
    const png = Buffer.from(gizmoImage.pngDataUrl.split(",", 2)[1], "base64");
    fs.writeFileSync(path.join(goldenDir, gizmoImage.file), png);
    if (
      [...protectedDigests].some(
        ([file, digest]) =>
          sha256(fs.readFileSync(path.join(goldenDir, file))) !== digest,
      )
    ) {
      throw new Error(
        "REGION3D-FLOOR gizmo amendment changed a protected golden byte",
      );
    }
    const amendmentFor = (key, inputs) => ({
      amendment: (expectedManifest[key]?.length || 0) + 1,
      reason:
        "REGION3D-FLOOR visual falsification replaced subpixel line-list gizmos with screen-space-thick triangle geometry; protected golden bytes remained unchanged",
      inputs,
    });
    const priorGizmo = expectedManifest.region3dFloorCases.find(
      (row) => row.caseId === "gizmo-overlay",
    );
    const currentGizmo = currentManifest.region3dFloorCases.find(
      (row) => row.caseId === "gizmo-overlay",
    );
    expectedManifest = {
      ...expectedManifest,
      imageAtomInputAmendments: [
        ...(expectedManifest.imageAtomInputAmendments || []),
        amendmentFor("imageAtomInputAmendments", currentManifest.imageAtomInputs),
      ],
      pathAtomInputAmendments: [
        ...(expectedManifest.pathAtomInputAmendments || []),
        amendmentFor("pathAtomInputAmendments", currentManifest.pathAtomInputs),
      ],
      connectorAtomInputAmendments: [
        ...(expectedManifest.connectorAtomInputAmendments || []),
        amendmentFor(
          "connectorAtomInputAmendments",
          currentManifest.connectorAtomInputs,
        ),
      ],
      chromeAtomInputAmendments: [
        ...(expectedManifest.chromeAtomInputAmendments || []),
        amendmentFor("chromeAtomInputAmendments", currentManifest.chromeAtomInputs),
      ],
      w4FrameRuntimeInputAmendments: [
        ...(expectedManifest.w4FrameRuntimeInputAmendments || []),
        amendmentFor(
          "w4FrameRuntimeInputAmendments",
          currentManifest.w4FrameRuntimeInputs,
        ),
      ],
      t2InputFloorInputAmendments: [
        ...(expectedManifest.t2InputFloorInputAmendments || []),
        amendmentFor(
          "t2InputFloorInputAmendments",
          currentManifest.t2InputFloorInputs,
        ),
      ],
      region3dFloorInputAmendments: [
        ...(expectedManifest.region3dFloorInputAmendments || []),
        amendmentFor(
          "region3dFloorInputAmendments",
          currentManifest.region3dFloorInputs,
        ),
      ],
      region3dFloorGoldenAmendments: [
        ...(expectedManifest.region3dFloorGoldenAmendments || []),
        {
          amendment:
            (expectedManifest.region3dFloorGoldenAmendments?.length || 0) + 1,
          reason:
            "Visual falsification made the representative gizmo overlay leg visibly exercise its contract",
          before: priorGizmo,
          after: currentGizmo,
        },
      ],
      region3dFloorCases: currentManifest.region3dFloorCases,
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
    expectedImageAtomInputs = currentManifest.imageAtomInputs;
    expectedPathAtomInputs = currentManifest.pathAtomInputs;
    expectedConnectorAtomInputs = currentManifest.connectorAtomInputs;
    expectedChromeAtomInputs = currentManifest.chromeAtomInputs;
    expectedW4FrameRuntimeInputs = currentManifest.w4FrameRuntimeInputs;
    expectedT2InputFloorInputs = currentManifest.t2InputFloorInputs;
    imageAtomInputsMatch = true;
    pathAtomInputsMatch = true;
    connectorAtomInputsMatch = true;
    chromeAtomInputsMatch = true;
    w4FrameRuntimeInputsMatch = true;
    t2InputFloorInputsMatch = true;
    expectedRegion3dFloorInputs = currentManifest.region3dFloorInputs;
    region3dFloorInputsMatch = true;
    region3dFloorComparison = compareGoldenRows(
      currentManifest.region3dFloorCases,
      expectedManifest.region3dFloorCases,
    );
    region3dFloorPass = comparisonPass(
      region3dFloorComparison,
      expectedManifest.region3dFloorCases,
      3,
    );
    chromeNamespaceCustodyRows = Object.keys(chromeSourceFiles).map((key) => ({
      key,
      expected: currentManifest.chromeAtomInputs[key]?.sha256 || null,
      current: currentManifest.chromeAtomInputs[key]?.sha256 || null,
      match: true,
    }));
    chromeNamespaceCustodyPass = chromeNamespaceCustodyRows.length === 6;
    pickRoadCustodyRows = [
      "imageAtomInputs",
      "pathAtomInputs",
      "connectorAtomInputs",
      "chromeAtomInputs",
      "w4FrameRuntimeInputs",
    ].flatMap((inputKey) =>
      ["sceneStore", "rectTree"].map((key) => ({
        inputKey,
        key,
        expected: currentManifest[inputKey]?.[key]?.sha256 || null,
        current: currentManifest[inputKey]?.[key]?.sha256 || null,
        match: true,
      })),
    );
    pickRoadCustodyPass = pickRoadCustodyRows.length === 10;
    region3dGizmoAmendmentPreflight.protectedGoldenBytesUnchanged = true;
  }
  const inputAmendmentPreflight = {
    bankPresent,
    sourceMatch,
    environmentMatch,
    legacyGoldensPass: imagePass,
    imageGoldensPass: imageAtomPass,
    pathGoldensPass: pathAtomPass,
    connectorGoldensPass: connectorAtomPass,
    chromeGoldensPass: chromeAtomPass,
    w4GoldensPass: w4FrameRuntimePass,
    legacyDeterminismPass: deterministic.length === 21 && determinismPass,
    imageDeterminismPass,
    pathDeterminismPass,
    connectorDeterminismPass,
    chromeDeterminismPass,
    w4DeterminismPass,
    imageParityPass,
    pathParityPass,
    connectorParityPass,
    imageContractPass: result.imageAtom.pass,
    pathContractPass: result.pathAtom.pass,
    connectorContractPass: result.connectorAtom.pass,
    chromeContractPass: result.chromeAtom.pass,
    w4ContractPass: result.w4FrameRuntime.pass,
    pathUploadGatePass: result.pathAtom.uploadGate?.pass,
    priorImageInputsMatch: imageAtomInputsMatch,
    priorPathInputsMatch: pathAtomInputsMatch,
    priorConnectorInputsMatch: connectorAtomInputsMatch,
    priorChromeInputsMatch: chromeAtomInputsMatch,
    priorW4InputsMatch: w4FrameRuntimeInputsMatch,
  };
  const inputAmendmentAuthorized =
    bankPresent &&
    sourceMatch &&
    environmentMatch &&
    imagePass &&
    imageAtomPass &&
    pathAtomPass &&
    connectorAtomPass &&
    chromeAtomPass &&
    w4FrameRuntimePass &&
    deterministic.length === 21 &&
    determinismPass &&
    imageDeterminismPass &&
    pathDeterminismPass &&
    connectorDeterminismPass &&
    chromeDeterminismPass &&
    w4DeterminismPass &&
    imageParityPass &&
    pathParityPass &&
    connectorParityPass &&
    result.imageAtom.pass &&
    result.pathAtom.pass &&
    result.connectorAtom.pass &&
    result.chromeAtom.pass &&
    result.w4FrameRuntime.pass &&
    chromeHybridPass &&
    result.chromeAtom.color?.pass &&
    result.chromeAtom.arrangement?.pass &&
    result.chromeAtom.uploadGate?.pass &&
    chromeAbsence.pass &&
    result.pathAtom.uploadGate?.pass;

  if (appendImageInputAmendment) {
    if (!inputAmendmentAuthorized) {
      throw new Error(
        `IMAGE-ATOM input amendment preflight failed: ${JSON.stringify(inputAmendmentPreflight)}`,
      );
    }
    expectedManifest = {
      ...expectedManifest,
      imageAtomInputAmendments: [
        ...(expectedManifest.imageAtomInputAmendments || []),
        {
          amendment: (expectedManifest.imageAtomInputAmendments?.length || 0) + 1,
          reason:
            "W4 shared-source integration changed scene flattening and verifier inputs with all image goldens byte-identical",
          inputs: currentManifest.imageAtomInputs,
        },
      ],
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
    expectedImageAtomInputs = currentManifest.imageAtomInputs;
    imageAtomInputsMatch = true;
  }

  if (appendPathInputAmendment) {
    if (!inputAmendmentAuthorized) {
      throw new Error(
        `PATH-ATOM input amendment preflight failed: ${JSON.stringify(inputAmendmentPreflight)}`,
      );
    }
    const amendment = {
      amendment: (expectedManifest.pathAtomInputAmendments?.length || 0) + 1,
      reason:
        "post-append falsification repaired mesh-set upload identity and froze its verifier receipt; all 45 golden files remain byte-identical",
      inputs: currentManifest.pathAtomInputs,
    };
    const imageAmendment = {
      amendment: (expectedManifest.imageAtomInputAmendments?.length || 0) + 1,
      reason:
        "PATH-ATOM source integration changed shared scene/verifier inputs with all 21 image-atom goldens and receipts unchanged",
      inputs: currentManifest.imageAtomInputs,
    };
    const connectorAmendment = {
      amendment:
        (expectedManifest.connectorAtomInputAmendments?.length || 0) + 1,
      reason:
        "PATH-ATOM input amendment refreshed shared verifier inputs with connector goldens unchanged",
      inputs: currentManifest.connectorAtomInputs,
    };
    expectedManifest = {
      ...expectedManifest,
      imageAtomInputAmendments: [
        ...(expectedManifest.imageAtomInputAmendments || []),
        imageAmendment,
      ],
      pathAtomInputAmendments: [
        ...(expectedManifest.pathAtomInputAmendments || []),
        amendment,
      ],
      connectorAtomInputAmendments: [
        ...(expectedManifest.connectorAtomInputAmendments || []),
        connectorAmendment,
      ],
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
    expectedImageAtomInputs = currentManifest.imageAtomInputs;
    expectedPathAtomInputs = currentManifest.pathAtomInputs;
    expectedConnectorAtomInputs = currentManifest.connectorAtomInputs;
    imageAtomInputsMatch = true;
    pathAtomInputsMatch = true;
    connectorAtomInputsMatch = true;
  }

  const connectorInputAmendmentPreflight = {
    ...inputAmendmentPreflight,
    connectorColorPass: result.connectorAtom.color?.pass,
    connectorArrangementPass: result.connectorAtom.arrangement?.pass,
    connectorLabelRoadPass: result.connectorAtom.labelRoad?.pass,
    connectorDarkLanePass: result.connectorAtom.darkLane?.pass,
    connectorUploadGatePass: result.connectorAtom.uploadGate?.pass,
  };
  const connectorInputAmendmentAuthorized =
    inputAmendmentAuthorized &&
    result.connectorAtom.color?.pass &&
    result.connectorAtom.arrangement?.pass &&
    result.connectorAtom.labelRoad?.pass &&
    result.connectorAtom.darkLane?.pass &&
    result.connectorAtom.uploadGate?.pass;

  if (appendConnectorInputAmendment) {
    if (!connectorInputAmendmentAuthorized) {
      throw new Error(
        `CONNECTOR-ATOM input amendment preflight failed: ${JSON.stringify(connectorInputAmendmentPreflight)}`,
      );
    }
    const imageAmendment = {
      amendment: (expectedManifest.imageAtomInputAmendments?.length || 0) + 1,
      reason:
        "CONNECTOR-ATOM post-append repair refreshed shared image inputs with all image goldens byte-identical",
      inputs: currentManifest.imageAtomInputs,
    };
    const pathAmendment = {
      amendment: (expectedManifest.pathAtomInputAmendments?.length || 0) + 1,
      reason:
        "CONNECTOR-ATOM post-append repair refreshed shared path inputs with all path goldens byte-identical",
      inputs: currentManifest.pathAtomInputs,
    };
    const connectorAmendment = {
      amendment:
        (expectedManifest.connectorAtomInputAmendments?.length || 0) + 1,
      reason:
        "CONNECTOR-ATOM post-append falsification repaired implementation inputs with all connector goldens byte-identical",
      inputs: currentManifest.connectorAtomInputs,
    };
    expectedManifest = {
      ...expectedManifest,
      imageAtomInputAmendments: [
        ...(expectedManifest.imageAtomInputAmendments || []),
        imageAmendment,
      ],
      pathAtomInputAmendments: [
        ...(expectedManifest.pathAtomInputAmendments || []),
        pathAmendment,
      ],
      connectorAtomInputAmendments: [
        ...(expectedManifest.connectorAtomInputAmendments || []),
        connectorAmendment,
      ],
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
    expectedImageAtomInputs = currentManifest.imageAtomInputs;
    expectedPathAtomInputs = currentManifest.pathAtomInputs;
    expectedConnectorAtomInputs = currentManifest.connectorAtomInputs;
    imageAtomInputsMatch = true;
    pathAtomInputsMatch = true;
    connectorAtomInputsMatch = true;
  }

  const chromeInputAmendmentPreflight = {
    ...inputAmendmentPreflight,
    chromeHybridPass,
    chromeColorPass: result.chromeAtom.color?.pass,
    chromeArrangementPass: result.chromeAtom.arrangement?.pass,
    chromeUploadGatePass: result.chromeAtom.uploadGate?.pass,
    chromeResourcesPass: result.chromeAtom.resources?.pass,
    chromeStaticAbsencePass: chromeAbsence.pass,
  };
  const chromeInputAmendmentAuthorized =
    inputAmendmentAuthorized &&
    chromeHybridPass &&
    result.chromeAtom.color?.pass &&
    result.chromeAtom.arrangement?.pass &&
    result.chromeAtom.uploadGate?.pass &&
    result.chromeAtom.resources?.pass &&
    chromeAbsence.pass;

  if (appendChromeInputAmendment) {
    if (!chromeInputAmendmentAuthorized) {
      throw new Error(
        `CHROME-ATOM input amendment preflight failed: ${JSON.stringify(chromeInputAmendmentPreflight)}`,
      );
    }
    const amendmentFor = (key, reason, inputs) => ({
      amendment: (expectedManifest[key]?.length || 0) + 1,
      reason,
      inputs,
    });
    expectedManifest = {
      ...expectedManifest,
      imageAtomInputAmendments: imageAtomInputsMatch
        ? expectedManifest.imageAtomInputAmendments
        : [
            ...(expectedManifest.imageAtomInputAmendments || []),
            amendmentFor(
              "imageAtomInputAmendments",
              "CHROME-ATOM post-append repair refreshed shared image inputs with all image goldens byte-identical",
              currentManifest.imageAtomInputs,
            ),
          ],
      pathAtomInputAmendments: pathAtomInputsMatch
        ? expectedManifest.pathAtomInputAmendments
        : [
            ...(expectedManifest.pathAtomInputAmendments || []),
            amendmentFor(
              "pathAtomInputAmendments",
              "CHROME-ATOM post-append repair refreshed shared path inputs with all path goldens byte-identical",
              currentManifest.pathAtomInputs,
            ),
          ],
      connectorAtomInputAmendments: connectorAtomInputsMatch
        ? expectedManifest.connectorAtomInputAmendments
        : [
            ...(expectedManifest.connectorAtomInputAmendments || []),
            amendmentFor(
              "connectorAtomInputAmendments",
              "CHROME-ATOM post-append repair refreshed shared connector inputs with all connector goldens byte-identical",
              currentManifest.connectorAtomInputs,
            ),
          ],
      chromeAtomInputAmendments: [
        ...(expectedManifest.chromeAtomInputAmendments || []),
        amendmentFor(
          "chromeAtomInputAmendments",
          "CHROME-ATOM post-append falsification repaired implementation inputs with all chrome goldens byte-identical",
          currentManifest.chromeAtomInputs,
        ),
      ],
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
    expectedImageAtomInputs = currentManifest.imageAtomInputs;
    expectedPathAtomInputs = currentManifest.pathAtomInputs;
    expectedConnectorAtomInputs = currentManifest.connectorAtomInputs;
    expectedChromeAtomInputs = currentManifest.chromeAtomInputs;
    imageAtomInputsMatch = true;
    pathAtomInputsMatch = true;
    connectorAtomInputsMatch = true;
    chromeAtomInputsMatch = true;
  }
  const w4InputAmendmentPreflight = {
    ...inputAmendmentPreflight,
    w4GoldensPass: w4FrameRuntimePass,
    w4DeterminismPass,
    w4CompositePass: result.w4FrameRuntime.composite?.pass,
    w4MixedContentPass: result.w4FrameRuntime.mixedContent?.pass,
    w4MaskPass: result.w4FrameRuntime.mask?.pass,
    w4GradientPass: result.w4FrameRuntime.gradient?.pass,
    w4ClipPass: result.w4FrameRuntime.clip?.pass,
    w4BlurPass: result.w4FrameRuntime.blur?.pass,
    w4SecondaryPass: result.w4FrameRuntime.secondary?.pass,
    w4ExportPass: result.w4FrameRuntime.export?.pass,
    w4SchedulerPass: result.w4FrameRuntime.scheduler?.pass,
  };
  const w4InputAmendmentAuthorized =
    inputAmendmentAuthorized &&
    w4FrameRuntimePass &&
    w4DeterminismPass &&
    result.w4FrameRuntime.composite?.pass &&
    result.w4FrameRuntime.mixedContent?.pass &&
    result.w4FrameRuntime.mask?.pass &&
    result.w4FrameRuntime.gradient?.pass &&
    result.w4FrameRuntime.clip?.pass &&
    result.w4FrameRuntime.blur?.pass &&
    result.w4FrameRuntime.secondary?.pass &&
    result.w4FrameRuntime.export?.pass &&
    result.w4FrameRuntime.scheduler?.pass &&
    result.w4FrameRuntime.pass;

  if (appendW4InputAmendment) {
    if (!w4InputAmendmentAuthorized) {
      throw new Error(
        `W4 FRAME-RUNTIME input amendment preflight failed: ${JSON.stringify(w4InputAmendmentPreflight)}`,
      );
    }
    expectedManifest = {
      ...expectedManifest,
      w4FrameRuntimeInputAmendments: [
        ...(expectedManifest.w4FrameRuntimeInputAmendments || []),
        {
          amendment:
            (expectedManifest.w4FrameRuntimeInputAmendments?.length || 0) + 1,
          reason:
            "W4 close refreshed its complete implementation and verifier input set after the bounded repair pass",
          inputs: currentManifest.w4FrameRuntimeInputs,
        },
      ],
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
    expectedW4FrameRuntimeInputs = currentManifest.w4FrameRuntimeInputs;
    w4FrameRuntimeInputsMatch = true;
  }
  const t2InputAmendmentPreflight = {
    ...inputAmendmentPreflight,
    t2GoldensPass: t2InputFloorPass,
    t2DeterminismPass,
    t2ClipPass: result.t2InputFloor.clip?.pass,
    t2OneResultPass: Object.values(result.t2InputFloor.oneResult || {}).every(
      (row) => row.pass,
    ),
    t2StaticAbsencePass: t2Absence.pass,
    chromeNamespaceCustodyPass,
    pickRoadCustodyPass,
    t2ContractPass: result.t2InputFloor.pass,
  };
  const t2InputAmendmentAuthorized =
    inputAmendmentAuthorized &&
    t2InputFloorPass &&
    t2DeterminismPass &&
    result.t2InputFloor.clip?.pass &&
    Object.values(result.t2InputFloor.oneResult || {}).length === 3 &&
    Object.values(result.t2InputFloor.oneResult || {}).every(
      (row) => row.pass,
    ) &&
    t2Absence.pass &&
    chromeNamespaceCustodyPass &&
    pickRoadCustodyPass &&
    result.t2InputFloor.pass;

  if (appendT2InputAmendment) {
    if (!t2InputAmendmentAuthorized) {
      throw new Error(
        `T2 INPUT-FLOOR input amendment preflight failed: ${JSON.stringify(t2InputAmendmentPreflight)}`,
      );
    }
    expectedManifest = {
      ...expectedManifest,
      t2InputFloorInputAmendments: [
        ...(expectedManifest.t2InputFloorInputAmendments || []),
        {
          amendment:
            (expectedManifest.t2InputFloorInputAmendments?.length || 0) + 1,
          reason:
            "T2 INPUT-FLOOR close refreshed its implementation and verifier input set with all golden bytes unchanged",
          inputs: currentManifest.t2InputFloorInputs,
        },
      ],
    };
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(expectedManifest, null, 2)}\n`,
    );
    expectedT2InputFloorInputs = currentManifest.t2InputFloorInputs;
    t2InputFloorInputsMatch = true;
  }
  const appendPostflightPass =
    (!appendImageGoldens ||
      (sourceMatch &&
        environmentMatch &&
        imagePass &&
        imageAtomPass &&
        imageAtomInputsMatch)) &&
    (!appendPathGoldens ||
      (sourceMatch &&
        environmentMatch &&
        imagePass &&
        imageAtomPass &&
        imageAtomInputsMatch &&
        pathAtomPass &&
        pathAtomInputsMatch)) &&
    (!appendW4Goldens ||
      (environmentMatch &&
        priorGoldenSetsPass &&
        w4FrameRuntimePass &&
        w4FrameRuntimeInputsMatch)) &&
    (!appendT2Goldens ||
      (environmentMatch &&
        priorT2GoldenSetsPass &&
        t2InputFloorPass &&
        t2InputFloorInputsMatch)) &&
    (!appendRegion3dGoldens ||
      (sourceMatch &&
        environmentMatch &&
        priorRegion3dGoldenSetsPass &&
        region3dFloorPass &&
        region3dFloorInputsMatch)) &&
    (!amendRegion3dGizmoGolden ||
      (region3dGizmoAmendmentAuthorized &&
        sourceMatch &&
        environmentMatch &&
        region3dFloorPass &&
        region3dFloorInputsMatch)) &&
    (!amendShaderDigests ||
      (shaderAmendmentAuthorized && sourceMatch && w4FrameRuntimePass)) &&
    (!appendImageInputAmendment ||
      (inputAmendmentAuthorized && imageAtomInputsMatch)) &&
    (!appendPathInputAmendment ||
      (inputAmendmentAuthorized &&
        imageAtomInputsMatch &&
        pathAtomInputsMatch &&
        connectorAtomInputsMatch)) &&
    (!appendConnectorGoldens ||
      (sourceMatch &&
        environmentMatch &&
        imagePass &&
        imageAtomPass &&
        imageAtomInputsMatch &&
        pathAtomPass &&
        pathAtomInputsMatch &&
        connectorAtomPass &&
        connectorAtomInputsMatch)) &&
    (!appendConnectorInputAmendment ||
      (connectorInputAmendmentAuthorized &&
        imageAtomInputsMatch &&
        pathAtomInputsMatch &&
        connectorAtomInputsMatch)) &&
    (!appendChromeGoldens ||
      (sourceMatch &&
        environmentMatch &&
        imagePass &&
        imageAtomPass &&
        imageAtomInputsMatch &&
        pathAtomPass &&
        pathAtomInputsMatch &&
        connectorAtomPass &&
        connectorAtomInputsMatch &&
        chromeAtomPass &&
        chromeAtomInputsMatch)) &&
    (!appendChromeInputAmendment ||
      (chromeInputAmendmentAuthorized &&
        imageAtomInputsMatch &&
        pathAtomInputsMatch &&
        connectorAtomInputsMatch &&
        chromeAtomInputsMatch)) &&
    (!appendW4InputAmendment ||
      (w4InputAmendmentAuthorized &&
        w4FrameRuntimePass &&
        w4FrameRuntimeInputsMatch)) &&
    (!appendT2InputAmendment ||
      (t2InputAmendmentAuthorized &&
        t2InputFloorPass &&
        t2InputFloorInputsMatch));
  if (!appendPostflightPass) {
    throw new Error("Scoped atom append postflight failed");
  }

  let classification = "pass";
  if (!bankPresent) classification = "missing-golden-bank";
  else if (!imagePass) classification = "existing-golden-byte-drift";
  else if (!environmentMatch) classification = "environment-mismatch";
  else if (!sourceMatch) classification = "production-source-mismatch";
  else if (!determinismPass) classification = "determinism-failure";
  else if (!q8TransportPass) classification = "q8-affine-transport-failure";
  else if (!q5AffineBoundaryPass)
    classification = "q5-affine-raster-boundary-failure";
  else if (
    !imageAtomPass ||
    !imageDeterminismPass ||
    !imageParityPass ||
    !imageAtomInputsMatch ||
    !result.imageAtom.pass
  )
    classification = "image-atom-contract-failure";
  else if (
    !pathAtomPass ||
    !pathDeterminismPass ||
    !pathParityPass ||
    !pathAtomInputsMatch ||
    !result.pathAtom.color?.pass ||
    !result.pathAtom.arrangement?.pass ||
    !result.pathAtom.darkLane?.pass ||
    !result.pathAtom.uploadGate?.pass ||
    !result.pathAtom.pass
  )
    classification = "path-atom-contract-failure";
  else if (
    !connectorAtomPass ||
    !connectorDeterminismPass ||
    !connectorParityPass ||
    !connectorAtomInputsMatch ||
    !result.connectorAtom.color?.pass ||
    !result.connectorAtom.arrangement?.pass ||
    !result.connectorAtom.labelRoad?.pass ||
    !result.connectorAtom.darkLane?.pass ||
    !result.connectorAtom.uploadGate?.pass ||
    !result.connectorAtom.pass
  )
    classification = "connector-atom-contract-failure";
  else if (
    !chromeAtomPass ||
    !chromeDeterminismPass ||
    !chromeAtomInputsMatch ||
    !chromeHybridPass ||
    !result.chromeAtom.color?.pass ||
    !result.chromeAtom.arrangement?.pass ||
    !result.chromeAtom.uploadGate?.pass ||
    !result.chromeAtom.resources?.pass ||
    !chromeAbsence.pass ||
    !result.chromeAtom.pass
  )
    classification = "chrome-atom-contract-failure";
  else if (
    !w4FrameRuntimePass ||
    !w4DeterminismPass ||
    !w4FrameRuntimeInputsMatch ||
    !result.w4FrameRuntime.composite?.pass ||
    !result.w4FrameRuntime.mixedContent?.pass ||
    !result.w4FrameRuntime.mask?.pass ||
    !result.w4FrameRuntime.gradient?.pass ||
    !result.w4FrameRuntime.clip?.pass ||
    !result.w4FrameRuntime.blur?.pass ||
    !result.w4FrameRuntime.secondary?.pass ||
    !result.w4FrameRuntime.aliasing?.pass ||
    !result.w4FrameRuntime.pool?.pass ||
    !result.w4FrameRuntime.export?.pass ||
    !result.w4FrameRuntime.scheduler?.pass ||
    !result.w4FrameRuntime.pass
  )
    classification = "w4-frame-runtime-contract-failure";
  else if (
    !t2InputFloorPass ||
    !t2DeterminismPass ||
    !t2InputFloorInputsMatch ||
    !result.t2InputFloor.clip?.pass ||
    !result.t2InputFloor.entry?.dblclickConsumed ||
    !result.t2InputFloor.outsideExit?.sessionClosed ||
    !result.t2InputFloor.outsideExit?.deadlineRetired ||
    !result.t2InputFloor.coexistence?.shiftPointerDefaultProceeded ||
    result.t2InputFloor.coexistence?.chromeSelection?.selected !== 1 ||
    !result.t2InputFloor.paragraph?.caretCaptured ||
    !result.t2InputFloor.blink?.cadence?.pass ||
    !result.t2InputFloor.dispatch?.keydownConsumed ||
    !result.t2InputFloor.dispatch?.pasteConsumed ||
    !chromeNamespaceCustodyPass ||
    !pickRoadCustodyPass ||
    !t2Absence.pass ||
    !result.t2InputFloor.timerSourceFree ||
    !result.t2InputFloor.pass
  )
    classification = "t2-input-floor-contract-failure";
  else if (
    !region3dFloorPass ||
    !region3dDeterminismPass ||
    !region3dFloorInputsMatch ||
    !result.region3dFloor.s1?.pass ||
    !result.region3dFloor.s2?.pass ||
    !result.region3dFloor.s3?.pass ||
    !result.region3dFloor.s4?.pass ||
    !result.region3dFloor.s5?.pass ||
    !result.region3dFloor.pass
  )
    classification = "region3d-floor-contract-failure";
  else if (!parityPass) classification = "candidate-pick-parity-failure";
  else if (!divergencePass)
    classification = "current-product-pick-sentinel-failure";

  const pass = classification === "pass";
  const msdfParity = parity.filter((row) => row.mode === "msdf-dejavu-o-path");
  const assertionFailures = [];
  const assertField = (condition, field) => {
    if (!condition) assertionFailures.push(field);
  };
  assertField(
    classification === "candidate-pick-parity-failure",
    "classification",
  );
  assertField(deterministic.length === 21 && determinismPass, "legacy-determinism");
  assertField(imagePass && imageComparison.length === 21, "legacy-goldens");
  assertField(
    parity.length === 21 && parity.filter((row) => row.pass).length === 14,
    "legacy-parity",
  );
  assertField(
    msdfParity.length === 7 &&
      msdfParity.every(
        (row) => row.mismatchCount === 47 && row.boundaryTieCount === 2,
      ),
    "msdf-counterexample-counts",
  );
  assertField(
    divergences.length === 7 && divergencePass,
    "divergence-sentinels",
  );
  assertField(q8TransportPass, "q8-affine-transport");
  assertField(q5AffineBoundaryPass, "q5-affine-boundary");
  assertField(sourceMatch, "source-match");
  assertField(environmentMatch, "environment-match");
  assertField(
    imageAtomPass && imageAtomComparison.length === 21,
    "image-goldens",
  );
  assertField(imageDeterminismPass, "image-determinism");
  assertField(imageParityPass, "image-parity-floors");
  assertField(imageAtomInputsMatch, "image-atom-inputs");
  assertField(result.imageAtom.color?.pass, "image-color");
  assertField(result.imageAtom.arrangement?.pass, "image-arrangement");
  assertField(result.imageAtom.lifecycle?.pass, "image-lifecycle");
  assertField(
    result.imageAtom.lifecycle?.unavailable?.status === "unavailable" &&
      result.imageAtom.lifecycle?.overBudget?.status === "refused" &&
      result.imageAtom.lifecycle?.overBudget?.reason === "over-budget" &&
      result.imageAtom.lifecycle?.deviceLoss?.status === "device-lost",
    "image-lifecycle-status-rows",
  );
  assertField(
    result.imageAtom.productLoopClaim === "dev-flagged" &&
      result.imageAtom.productLoopJoin === "live-atoms" &&
      result.imageAtom.defaultDark === true,
    "product-loop-live-atoms-join",
  );
  assertField(
    pathAtomPass && pathAtomComparison.length === 3,
    "path-goldens",
  );
  assertField(pathDeterminismPass, "path-determinism");
  assertField(pathParityPass, "path-parity");
  assertField(pathAtomInputsMatch, "path-atom-inputs");
  assertField(result.pathAtom.color?.pass, "path-color");
  assertField(result.pathAtom.arrangement?.pass, "path-arrangement");
  assertField(result.pathAtom.darkLane?.pass, "path-dark-lane");
  assertField(result.pathAtom.uploadGate?.pass, "path-upload-gate");
  assertField(
    result.pathAtom.productPick === "cpu-path-authority" &&
      result.pathAtom.coverage === "aliased-v1" &&
      result.pathAtom.selfOverlapAlpha ===
        "direct-triangle-double-blend-declared" &&
      result.pathAtom.pass,
    "path-contract",
  );
  assertField(
    connectorAtomPass && connectorAtomComparison.length === 3,
    "connector-goldens",
  );
  assertField(connectorDeterminismPass, "connector-determinism");
  assertField(connectorParityPass, "connector-parity");
  assertField(connectorAtomInputsMatch, "connector-atom-inputs");
  assertField(result.connectorAtom.color?.pass, "connector-color");
  assertField(
    result.connectorAtom.arrangement?.pass,
    "connector-arrangement",
  );
  assertField(result.connectorAtom.labelRoad?.pass, "connector-label-road");
  assertField(result.connectorAtom.darkLane?.pass, "connector-dark-lane");
  assertField(result.connectorAtom.uploadGate?.pass, "connector-upload-gate");
  assertField(
    result.connectorAtom.productPick === "cpu-connector-authority" &&
      result.connectorAtom.coverage === "aliased-v1" &&
      result.connectorAtom.pass,
    "connector-contract",
  );
  assertField(
    chromeAtomPass && chromeAtomComparison.length === 3,
    "chrome-goldens",
  );
  assertField(chromeDeterminismPass, "chrome-determinism");
  assertField(chromeAtomInputsMatch, "chrome-atom-inputs");
  assertField(chromeHybridPass, "chrome-hybrid-metric");
  assertField(result.chromeAtom.color?.pass, "chrome-color");
  assertField(result.chromeAtom.arrangement?.pass, "chrome-arrangement");
  assertField(result.chromeAtom.uploadGate?.pass, "chrome-upload-gate");
  assertField(result.chromeAtom.resources?.pass, "chrome-resources");
  assertField(chromeAbsence.pass, "chrome-static-absence");
  assertField(
    result.chromeAtom.coverage === "aliased-v1" && result.chromeAtom.pass,
    "chrome-contract",
  );
  assertField(
    w4FrameRuntimePass && w4FrameRuntimeComparison.length === 3,
    "w4-frame-runtime-goldens",
  );
  assertField(w4DeterminismPass, "w4-frame-runtime-determinism");
  assertField(w4FrameRuntimeInputsMatch, "w4-frame-runtime-inputs");
  assertField(result.w4FrameRuntime.composite?.pass, "w4-composite");
  assertField(result.w4FrameRuntime.mixedContent?.pass, "w4-mixed-content");
  assertField(result.w4FrameRuntime.mask?.pass, "w4-mask");
  assertField(result.w4FrameRuntime.gradient?.pass, "w4-gradient");
  assertField(result.w4FrameRuntime.clip?.pass, "w4-clip");
  assertField(result.w4FrameRuntime.blur?.pass, "w4-blur");
  assertField(result.w4FrameRuntime.secondary?.pass, "w4-secondary");
  assertField(result.w4FrameRuntime.aliasing?.pass, "w4-aliasing");
  assertField(result.w4FrameRuntime.pool?.pass, "w4-target-pool");
  assertField(result.w4FrameRuntime.export?.pass, "w4-export");
  assertField(result.w4FrameRuntime.scheduler?.pass, "w4-scheduler");
  assertField(result.w4FrameRuntime.pass, "w4-frame-runtime-contract");
  assertField(
    t2InputFloorPass && t2InputFloorComparison.length === 3,
    "t2-input-floor-goldens",
  );
  assertField(t2DeterminismPass, "t2-input-floor-determinism");
  assertField(t2InputFloorInputsMatch, "t2-input-floor-inputs");
  assertField(
    Object.values(result.t2InputFloor.oneResult || {}).length === 3 &&
      Object.values(result.t2InputFloor.oneResult || {}).every(
        (row) =>
          row.pass && row.valueEqualLines && row.paintLayoutIds?.length > 0,
      ),
    "t2-one-result-identity",
  );
  assertField(
    JSON.stringify(
      (result.t2InputFloor.transitions || []).map((row) => row.seamDelta),
    ) === JSON.stringify([1, 1, 1, 1, 0]) &&
      result.t2InputFloor.transitions?.[1]?.semanticOps === 1,
    "t2-one-seam-transitions",
  );
  assertField(
    result.t2InputFloor.clip?.pass &&
      result.t2InputFloor.clip?.source === "production-store-metadata" &&
      JSON.stringify(result.t2InputFloor.clip?.deviceScaleProbe) ===
        JSON.stringify({ mode: "scissor", x: 40, y: 0, w: 216, h: 256 }) &&
      result.t2InputFloor.clip?.outsideProbeMax === 0 &&
      result.t2InputFloor.clip?.insideProbeMax > 32,
    "t2-clip-probes",
  );
  assertField(
    result.t2InputFloor.paragraph?.caretCaptured &&
      result.t2InputFloor.paragraph?.spansLigatureAndRtl &&
      result.t2InputFloor.paragraph?.containment?.initial?.pass &&
      result.t2InputFloor.paragraph?.containment?.afterPaste?.pass &&
      result.t2InputFloor.paragraph?.containment?.afterPaste?.rootHeight >=
        result.t2InputFloor.paragraph?.containment?.initial?.rootHeight &&
      JSON.stringify(result.t2InputFloor.paragraph?.selectionSourceRange) ===
        JSON.stringify([12, 166]) &&
      result.t2InputFloor.paragraph?.caretCapturePoint?.every(
        (coordinate) => coordinate >= 0 && coordinate < 128,
      ),
    "t2-paragraph-caret-capture",
  );
  assertField(
    result.t2InputFloor.darkLane?.flagOff &&
      !result.t2InputFloor.darkLane?.receiptBefore &&
      result.t2InputFloor.darkLane?.pureLoad,
    "t2-dark-lane",
  );
  assertField(
    result.t2InputFloor.segmenter?.supported &&
      result.t2InputFloor.segmenter?.identity?.api === "intl-segmenter",
    "t2-segmenter",
  );
  assertField(
    result.t2InputFloor.ime?.hiddenTextarea &&
      result.t2InputFloor.ime?.focusedDuringSession &&
      result.t2InputFloor.ime?.screenPoint?.length === 2 &&
      result.t2InputFloor.ime?.oneCommitOneOp,
    "t2-ime-host",
  );
  assertField(
    result.t2InputFloor.dispatch?.keydownConsumed &&
      result.t2InputFloor.dispatch?.keydownDefaultPrevented &&
      result.t2InputFloor.dispatch?.keydownIntercepts === 3 &&
      result.t2InputFloor.dispatch?.copyConsumed &&
      result.t2InputFloor.dispatch?.copyDefaultPrevented &&
      result.t2InputFloor.dispatch?.clipboardWrite?.ok &&
      result.t2InputFloor.dispatch?.clipboardWrite?.text ===
        result.t2InputFloor.dispatch?.copyExpected &&
      result.t2InputFloor.dispatch?.clipboardAuthority ===
        "browser-system-clipboard-with-granted-permission" &&
      result.t2InputFloor.dispatch?.pasteConsumed &&
      result.t2InputFloor.dispatch?.pasteDefaultPrevented &&
      result.t2InputFloor.dispatch?.pasteIntercepts === 1 &&
      result.t2InputFloor.dispatch?.pasteNormalizedText === "a\nb\t",
    "t2-dispatch-intercepts",
  );
  assertField(
    result.t2InputFloor.entry?.target === "paragraph" &&
      result.t2InputFloor.entry?.dblclickConsumed,
    "t2-entry",
  );
  assertField(
    result.t2InputFloor.coexistence?.shiftPointerDefaultProceeded &&
      result.t2InputFloor.coexistence?.sessionPassThroughs > 0 &&
      result.t2InputFloor.coexistence?.chromeSelection?.selected === 1 &&
      result.t2InputFloor.coexistence?.selectedIdentity?.vi ===
        "clipped-card",
    "t2-shift-pass-through",
  );
  assertField(
    result.t2InputFloor.outsideExit?.defaultProceeded &&
      result.t2InputFloor.outsideExit?.sessionClosed &&
      result.t2InputFloor.outsideExit?.deadlineRetired,
    "t2-outside-exit",
  );
  assertField(
    result.t2InputFloor.blink?.armed &&
      result.t2InputFloor.blink?.toggles > 0 &&
      result.t2InputFloor.blink?.layoutDelta === 0 &&
      result.t2InputFloor.blink?.cadence?.pass &&
      result.t2InputFloor.blink?.cadence?.dueCount === 9 &&
      result.t2InputFloor.blink?.cadence?.echoEncodes === 0 &&
      result.t2InputFloor.runtime?.dueConsumerInstalled,
    "t2-blink-deadline",
  );
  assertField(
    result.t2InputFloor.runtime?.providerShaped &&
      result.t2InputFloor.runtime?.interceptsInstalled &&
      result.t2InputFloor.runtime?.census?.containerStable,
    "t2-runtime-install",
  );
  assertField(
    result.t2InputFloor.exportDeviation?.worldSessionVisuals > 0 &&
      result.t2InputFloor.exportDeviation?.repaymentRoad ===
        "editing-chrome-expansion",
    "t2-export-deviation",
  );
  assertField(result.t2InputFloor.timerSourceFree, "t2-timer-source-free");
  assertField(t2Absence.pass, "t2-static-timer-absence");
  assertField(chromeNamespaceCustodyPass, "t2-chrome-namespace-custody");
  assertField(pickRoadCustodyPass, "t2-pick-road-custody");
  assertField(result.t2InputFloor.pass, "t2-input-floor-contract");
  assertField(
    region3dFloorPass && region3dFloorComparison.length === 3,
    "region3d-floor-goldens",
  );
  assertField(region3dDeterminismPass, "region3d-floor-determinism");
  assertField(region3dFloorInputsMatch, "region3d-floor-inputs");
  assertField(result.region3dFloor.s1?.pass, "region3d-s1-sandwich");
  assertField(
    result.region3dFloor.s2?.pass &&
      result.region3dFloor.s2?.actualObject === "near" &&
      result.region3dFloor.s2?.maxByteDelta <= 2 &&
      result.region3dFloor.s2?.transparencyRgba?.[3] > 0 &&
      result.region3dFloor.s2?.transparencyRgba?.[3] < 255,
    "region3d-s2-pick-depth-transparency",
  );
  assertField(
    result.region3dFloor.s3?.pass &&
      result.region3dFloor.s3?.diffCount === 1 &&
      !result.region3dFloor.s3?.sceneReplacement &&
      result.region3dFloor.s3?.replayIdentical,
    "region3d-s3-one-diff-replay",
  );
  assertField(
    result.region3dFloor.s4?.pass &&
      result.region3dFloor.s4?.maxByteDelta <= 2,
    "region3d-s4-pbr-oracle",
  );
  assertField(
    result.region3dFloor.s5?.pass &&
      result.region3dFloor.s5?.cameraWake?.objectInstanceUploadDelta === 0 &&
      result.region3dFloor.s5?.cameraWake?.meshVertexUploadDelta === 0 &&
      result.region3dFloor.s5?.cameraWake?.uniformUploadDelta === 1 &&
      result.region3dFloor.s5?.cleanHeld &&
      Object.values(result.region3dFloor.s5?.resize?.leases || {}).length === 1 &&
      result.region3dFloor.s5?.shadowOff?.bytes <
        result.region3dFloor.s5?.resize?.bytes &&
      result.region3dFloor.s5?.afterClose?.bytes === 0 &&
      result.region3dFloor.s5?.refusal?.pass,
    "region3d-s5-lifecycle-refusal",
  );
  assertField(result.region3dFloor.pass, "region3d-floor-contract");
  assertField(
    Boolean(
      attestation.adapterIdentity.vendor &&
        typeof attestation.isFallbackAdapter === "boolean" &&
        attestation.renderer,
    ),
    "attestation",
  );

  const stdoutSummary = {
    pass,
    classification,
    receipt: path.relative(repoRoot, receiptFile),
    images: currentManifest.images.length,
    deterministic: `${deterministic.filter((row) => row.byteIdentical).length}/${deterministic.length}`,
    existingGoldens: `${imageComparison.filter((row) => row.rawMatch && row.pngManifestMatch && row.goldenFileMatch).length}/${imageComparison.length}`,
    sourceMatch,
    environmentMatch,
    imageGoldens: `${imageAtomComparison.filter((row) => row.rawMatch && row.pngManifestMatch && row.goldenFileMatch).length}/${imageAtomComparison.length}`,
    imageDeterminism: `${imageDeterministic.filter((row) => row.byteIdentical).length}/${imageDeterministic.length}`,
    imageParity: `${imageParity.filter((row) => row.pass && row.boundaryPixelCount > 0 && row.decisiveCount > 0).length}/${imageParity.length}`,
    pathGoldens: `${pathAtomComparison.filter((row) => row.rawMatch && row.pngManifestMatch && row.goldenFileMatch).length}/${pathAtomComparison.length}`,
    pathDeterminism: `${pathDeterministic.filter((row) => row.byteIdentical).length}/${pathDeterministic.length}`,
    pathParity: `${pathParity.filter((row) => row.pass && row.mismatchCount === 0 && row.boundaryPixelCount > 0 && row.decisiveCount > 0).length}/${pathParity.length}`,
    pathColor: Boolean(result.pathAtom.color?.pass),
    pathArrangement: Boolean(result.pathAtom.arrangement?.pass),
    pathDarkLane: Boolean(result.pathAtom.darkLane?.pass),
    pathUploadGate: Boolean(result.pathAtom.uploadGate?.pass),
    connectorGoldens: `${connectorAtomComparison.filter((row) => row.rawMatch && row.pngManifestMatch && row.goldenFileMatch).length}/${connectorAtomComparison.length}`,
    connectorDeterminism: `${connectorDeterministic.filter((row) => row.byteIdentical).length}/${connectorDeterministic.length}`,
    connectorParity: `${connectorParity.filter((row) => row.pass && row.mismatchCount === 0 && row.boundaryPixelCount > 0 && row.decisiveCount > 0).length}/${connectorParity.length}`,
    connectorColor: Boolean(result.connectorAtom.color?.pass),
    connectorArrangement: Boolean(result.connectorAtom.arrangement?.pass),
    connectorLabelRoad: Boolean(result.connectorAtom.labelRoad?.pass),
    connectorDarkLane: Boolean(result.connectorAtom.darkLane?.pass),
    connectorUploadGate: Boolean(result.connectorAtom.uploadGate?.pass),
    chromeGoldens: `${chromeAtomComparison.filter((row) => row.rawMatch && row.pngManifestMatch && row.goldenFileMatch).length}/${chromeAtomComparison.length}`,
    chromeDeterminism: `${chromeDeterministic.filter((row) => row.byteIdentical).length}/${chromeDeterministic.length}`,
    chromeHybridMetric: chromeHybridPass,
    chromeColor: Boolean(result.chromeAtom.color?.pass),
    chromeArrangement: Boolean(result.chromeAtom.arrangement?.pass),
    chromeUploadGate: Boolean(result.chromeAtom.uploadGate?.pass),
    chromeResources: Boolean(result.chromeAtom.resources?.pass),
    chromeStaticAbsence: chromeAbsence.pass,
    w4Goldens: `${w4FrameRuntimeComparison.filter((row) => row.rawMatch && row.pngManifestMatch && row.goldenFileMatch).length}/${w4FrameRuntimeComparison.length}`,
    w4Determinism: `${w4Deterministic.filter((row) => row.byteIdentical).length}/${w4Deterministic.length}`,
    w4Composite: Boolean(result.w4FrameRuntime.composite?.pass),
    w4MixedContent: Boolean(result.w4FrameRuntime.mixedContent?.pass),
    w4Mask: Boolean(result.w4FrameRuntime.mask?.pass),
    w4Gradient: Boolean(result.w4FrameRuntime.gradient?.pass),
    w4Clip: Boolean(result.w4FrameRuntime.clip?.pass),
    w4Blur: Boolean(result.w4FrameRuntime.blur?.pass),
    w4Secondary: Boolean(result.w4FrameRuntime.secondary?.pass),
    w4Aliasing: Boolean(result.w4FrameRuntime.aliasing?.pass),
    w4TargetPool: Boolean(result.w4FrameRuntime.pool?.pass),
    w4Export: Boolean(result.w4FrameRuntime.export?.pass),
    w4Scheduler: Boolean(result.w4FrameRuntime.scheduler?.pass),
    t2Goldens: `${t2InputFloorComparison.filter((row) => row.rawMatch && row.pngManifestMatch && row.goldenFileMatch).length}/${t2InputFloorComparison.length}`,
    t2Determinism: `${t2Deterministic.filter((row) => row.byteIdentical).length}/${t2Deterministic.length}`,
    t2Inputs: t2InputFloorInputsMatch,
    t2OneResult: Object.values(result.t2InputFloor.oneResult || {}).every(
      (row) => row.pass,
    ),
    t2Clip: Boolean(result.t2InputFloor.clip?.pass),
    t2ParagraphCaret: Boolean(result.t2InputFloor.paragraph?.caretCaptured),
    t2Ime: Boolean(result.t2InputFloor.ime?.oneCommitOneOp),
    t2Dispatch: Boolean(
      result.t2InputFloor.dispatch?.keydownConsumed &&
        result.t2InputFloor.dispatch?.pasteConsumed,
    ),
    t2Entry: Boolean(result.t2InputFloor.entry?.dblclickConsumed),
    t2OutsideExit: Boolean(result.t2InputFloor.outsideExit?.sessionClosed),
    t2DeadlineRetired: Boolean(
      result.t2InputFloor.outsideExit?.deadlineRetired,
    ),
    t2BlinkCadence: Boolean(result.t2InputFloor.blink?.cadence?.pass),
    t2ShiftPassThrough: Boolean(
      result.t2InputFloor.coexistence?.shiftPointerDefaultProceeded,
    ),
    t2TimerSourceFree: Boolean(result.t2InputFloor.timerSourceFree),
    t2StaticTimerAbsence: t2Absence.pass,
    t2ChromeNamespaceCustody: chromeNamespaceCustodyPass,
    t2PickRoadCustody: pickRoadCustodyPass,
    region3dGoldens: `${region3dFloorComparison.filter((row) => row.rawMatch && row.pngManifestMatch && row.goldenFileMatch).length}/${region3dFloorComparison.length}`,
    region3dDeterminism: `${region3dDeterministic.filter((row) => row.byteIdentical).length}/${region3dDeterministic.length}`,
    region3dInputs: region3dFloorInputsMatch,
    region3dS1: Boolean(result.region3dFloor.s1?.pass),
    region3dS2: Boolean(result.region3dFloor.s2?.pass),
    region3dS3: Boolean(result.region3dFloor.s3?.pass),
    region3dS4: Boolean(result.region3dFloor.s4?.pass),
    region3dS5: Boolean(result.region3dFloor.s5?.pass),
    q8AffineTransport: q8TransportPass,
    q5AffineRasterBoundary: q5AffineBoundaryPass,
    candidateParity: `${parity.filter((row) => row.pass).length}/${parity.length}`,
    productBoundsDivergenceSentinels: `${divergences.filter((row) => row.expectedDivergence).length}/${divergences.length}`,
    appendImageGoldens,
    appendAuthorized,
    appendPathGoldens,
    pathAppendAuthorized,
    appendConnectorGoldens,
    connectorAppendAuthorized,
    appendChromeGoldens,
    chromeAppendAuthorized,
    appendW4Goldens,
    w4AppendAuthorized,
    appendT2Goldens,
    t2AppendAuthorized,
    appendRegion3dGoldens,
    region3dAppendAuthorized,
    amendRegion3dGizmoGolden,
    region3dGizmoAmendmentAuthorized,
    amendT2LivedCorrection,
    t2LivedCorrectionAuthorized,
    appendImageInputAmendment,
    appendPathInputAmendment,
    inputAmendmentAuthorized,
    appendConnectorInputAmendment,
    connectorInputAmendmentAuthorized,
    appendChromeInputAmendment,
    chromeInputAmendmentAuthorized,
    appendW4InputAmendment,
    w4InputAmendmentAuthorized,
    appendT2InputAmendment,
    t2InputAmendmentAuthorized,
    amendShaderDigests,
    shaderAmendmentAuthorized,
    assertImageContract,
    assertPathContract,
    assertConnectorContract,
    assertChromeContract,
    assertW4Contract,
    assertT2Contract,
    assertRegion3dContract,
    assertionPass: assertionFailures.length === 0,
    assertionFailures,
    environmentFingerprint: environment.fingerprintSha256,
  };
  for (const [key, value] of Object.entries({
    existingGoldens: "21/21",
    sourceMatch: true,
    environmentMatch: true,
    imageGoldens: "21/21",
    imageDeterminism: "21/21",
    imageParity: "14/14",
    pathGoldens: "3/3",
    pathDeterminism: "3/3",
    pathParity: "7/7",
    pathColor: true,
    pathArrangement: true,
    pathDarkLane: true,
    pathUploadGate: true,
    connectorGoldens: "3/3",
    connectorDeterminism: "3/3",
    connectorParity: "7/7",
    connectorColor: true,
    connectorArrangement: true,
    connectorLabelRoad: true,
    connectorDarkLane: true,
    connectorUploadGate: true,
    chromeGoldens: "3/3",
    chromeDeterminism: "3/3",
    chromeHybridMetric: true,
    chromeColor: true,
    chromeArrangement: true,
    chromeUploadGate: true,
    chromeResources: true,
    chromeStaticAbsence: true,
    w4Goldens: "3/3",
    w4Determinism: "3/3",
    w4Composite: true,
    w4MixedContent: true,
    w4Mask: true,
    w4Gradient: true,
    w4Clip: true,
    w4Blur: true,
    w4Secondary: true,
    w4Aliasing: true,
    w4TargetPool: true,
    w4Export: true,
    w4Scheduler: true,
    t2Goldens: "3/3",
    t2Determinism: "3/3",
    t2Inputs: true,
    t2OneResult: true,
    t2Clip: true,
    t2ParagraphCaret: true,
    t2Ime: true,
    t2Dispatch: true,
    t2Entry: true,
    t2OutsideExit: true,
    t2DeadlineRetired: true,
    t2BlinkCadence: true,
    t2ShiftPassThrough: true,
    t2TimerSourceFree: true,
    t2StaticTimerAbsence: true,
    t2ChromeNamespaceCustody: true,
    t2PickRoadCustody: true,
    region3dGoldens: "3/3",
    region3dDeterminism: "3/3",
    region3dInputs: true,
    region3dS1: true,
    region3dS2: true,
    region3dS3: true,
    region3dS4: true,
    region3dS5: true,
  })) {
    assertField(
      Object.hasOwn(stdoutSummary, key) && stdoutSummary[key] === value,
      `stdout-${key}`,
    );
  }
  stdoutSummary.assertionPass = assertionFailures.length === 0;
  stdoutSummary.assertionFailures = assertionFailures;

  const receipt = {
    attestation,
    schemaVersion: 1,
    verifier: result.verifier,
    replayCommand: "npm run verify:render-engine",
    updateCommand:
      "clj -M:dev -m shadow.cljs.devtools.cli release render-verifier && node test/render_engine/run_verifier.mjs --append-region3d-goldens --assert-region3d-contract",
    pass,
    classification,
    bankPresent,
    updateRequested:
      appendImageGoldens ||
      appendPathGoldens ||
      appendConnectorGoldens ||
      appendChromeGoldens ||
      appendW4Goldens ||
      appendT2Goldens ||
      appendRegion3dGoldens ||
      amendRegion3dGizmoGolden ||
      amendT2LivedCorrection ||
      appendImageInputAmendment ||
      appendPathInputAmendment ||
      appendConnectorInputAmendment ||
      appendChromeInputAmendment ||
      appendW4InputAmendment ||
      appendT2InputAmendment ||
      amendShaderDigests,
    updateAuthorized: appendImageGoldens
      ? appendAuthorized
      : appendPathGoldens
        ? pathAppendAuthorized
        : appendConnectorGoldens
          ? connectorAppendAuthorized
        : appendChromeGoldens
          ? chromeAppendAuthorized
        : appendW4Goldens
          ? w4AppendAuthorized
        : appendT2Goldens
          ? t2AppendAuthorized
        : appendRegion3dGoldens
          ? region3dAppendAuthorized
        : amendRegion3dGizmoGolden
          ? region3dGizmoAmendmentAuthorized
        : amendT2LivedCorrection
          ? t2LivedCorrectionAuthorized
        : amendShaderDigests
          ? shaderAmendmentAuthorized
        : appendImageInputAmendment
          ? inputAmendmentAuthorized
        : appendPathInputAmendment
          ? inputAmendmentAuthorized
          : appendConnectorInputAmendment
            ? connectorInputAmendmentAuthorized
          : appendChromeInputAmendment
            ? chromeInputAmendmentAuthorized
          : appendW4InputAmendment
            ? w4InputAmendmentAuthorized
          : appendT2InputAmendment
            ? t2InputAmendmentAuthorized
        : updateAuthorized,
    updateAuthority:
      "scoped atom appends require unchanged prior golden bytes, deterministic new pixels, contract receipts, and the preserved independent MSDF RED",
    sourceMatch,
    environmentMatch,
    environment,
    currentManifest,
    determinism: {
      pass: determinismPass,
      rows: deterministic,
    },
    q8AffineTransport: result.q8Transport,
    q5AffineRasterBoundary: result.q5AffineBoundary,
    candidatePickParity: {
      pass: parityPass,
      currentProductPick: false,
      rows: parity,
    },
    currentProductBoundsPickDivergence: {
      pass: divergencePass,
      rows: divergences,
    },
    goldenComparison: {
      pass: imagePass,
      rows: imageComparison,
    },
    imageAtom: {
      pass: result.imageAtom.pass,
      inputFingerprintsMatch: imageAtomInputsMatch,
      determinism: { pass: imageDeterminismPass, rows: imageDeterministic },
      pickParity: { pass: imageParityPass, rows: imageParity },
      goldenComparison: { pass: imageAtomPass, rows: imageAtomComparison },
      color: result.imageAtom.color,
      arrangement: result.imageAtom.arrangement,
      lifecycle: result.imageAtom.lifecycle,
      productLoopClaim: result.imageAtom.productLoopClaim,
      productLoopJoin: result.imageAtom.productLoopJoin,
      imageAboveTextKindLayer: result.imageAtom.imageAboveTextKindLayer,
      defaultDark: result.imageAtom.defaultDark,
      feltGate: result.imageAtom.feltGate,
    },
    pathAtom: {
      pass: result.pathAtom.pass,
      inputFingerprintsMatch: pathAtomInputsMatch,
      determinism: { pass: pathDeterminismPass, rows: pathDeterministic },
      pickParity: { pass: pathParityPass, rows: pathParity },
      goldenComparison: { pass: pathAtomPass, rows: pathAtomComparison },
      color: result.pathAtom.color,
      arrangement: result.pathAtom.arrangement,
      darkLane: result.pathAtom.darkLane,
      uploadGate: result.pathAtom.uploadGate,
      system: result.pathAtom.system,
      coverage: result.pathAtom.coverage,
      productPick: result.pathAtom.productPick,
      selfOverlapAlpha: result.pathAtom.selfOverlapAlpha,
    },
    connectorAtom: {
      pass: result.connectorAtom.pass,
      inputFingerprintsMatch: connectorAtomInputsMatch,
      determinism: {
        pass: connectorDeterminismPass,
        rows: connectorDeterministic,
      },
      pickParity: { pass: connectorParityPass, rows: connectorParity },
      goldenComparison: {
        pass: connectorAtomPass,
        rows: connectorAtomComparison,
      },
      color: result.connectorAtom.color,
      arrangement: result.connectorAtom.arrangement,
      labelRoad: result.connectorAtom.labelRoad,
      darkLane: result.connectorAtom.darkLane,
      uploadGate: result.connectorAtom.uploadGate,
      system: result.connectorAtom.system,
      geometry: result.connectorAtom.geometry,
      coverage: result.connectorAtom.coverage,
      productPick: result.connectorAtom.productPick,
    },
    chromeAtom: {
      pass: result.chromeAtom.pass,
      inputFingerprintsMatch: chromeAtomInputsMatch,
      determinism: {
        pass: chromeDeterminismPass,
        rows: chromeDeterministic,
      },
      goldenComparison: {
        pass: chromeAtomPass,
        rows: chromeAtomComparison,
      },
      hybridMetric: result.chromeAtom.hybridMetric,
      color: result.chromeAtom.color,
      arrangement: result.chromeAtom.arrangement,
      uploadGate: result.chromeAtom.uploadGate,
      resources: result.chromeAtom.resources,
      staticAbsence: chromeAbsence,
      system: result.chromeAtom.system,
      coverage: result.chromeAtom.coverage,
    },
    w4FrameRuntime: {
      pass: result.w4FrameRuntime.pass,
      inputFingerprintsMatch: w4FrameRuntimeInputsMatch,
      determinism: { pass: w4DeterminismPass, rows: w4Deterministic },
      goldenComparison: {
        pass: w4FrameRuntimePass,
        rows: w4FrameRuntimeComparison,
      },
      composite: result.w4FrameRuntime.composite,
      mixedContent: result.w4FrameRuntime.mixedContent,
      mask: result.w4FrameRuntime.mask,
      gradient: result.w4FrameRuntime.gradient,
      clip: result.w4FrameRuntime.clip,
      blur: result.w4FrameRuntime.blur,
      secondary: result.w4FrameRuntime.secondary,
      aliasing: result.w4FrameRuntime.aliasing,
      pool: result.w4FrameRuntime.pool,
      formats: result.w4FrameRuntime.formats,
      export: result.w4FrameRuntime.export,
      scheduler: result.w4FrameRuntime.scheduler,
      compositor: result.w4FrameRuntime.compositor,
    },
    t2InputFloor: {
      pass: result.t2InputFloor.pass,
      inputFingerprintsMatch: t2InputFloorInputsMatch,
      determinism: { pass: t2DeterminismPass, rows: t2Deterministic },
      goldenComparison: {
        pass: t2InputFloorPass,
        rows: t2InputFloorComparison,
      },
      oneResult: result.t2InputFloor.oneResult,
      transitions: result.t2InputFloor.transitions,
      clip: result.t2InputFloor.clip,
      paragraph: result.t2InputFloor.paragraph,
      darkLane: result.t2InputFloor.darkLane,
      entry: result.t2InputFloor.entry,
      outsideExit: result.t2InputFloor.outsideExit,
      coexistence: result.t2InputFloor.coexistence,
      segmenter: result.t2InputFloor.segmenter,
      ime: result.t2InputFloor.ime,
      dispatch: result.t2InputFloor.dispatch,
      blink: result.t2InputFloor.blink,
      timerSourceFree: result.t2InputFloor.timerSourceFree,
      staticAbsence: t2Absence,
      exportDeviation: result.t2InputFloor.exportDeviation,
      runtime: result.t2InputFloor.runtime,
      chromeNamespaceCustody: {
        pass: chromeNamespaceCustodyPass,
        rows: chromeNamespaceCustodyRows,
      },
      pickRoadCustody: {
        pass: pickRoadCustodyPass,
        rows: pickRoadCustodyRows,
      },
    },
    region3dFloor: {
      pass: result.region3dFloor.pass,
      inputFingerprintsMatch: region3dFloorInputsMatch,
      determinism: {
        pass: region3dDeterminismPass,
        rows: region3dDeterministic,
      },
      goldenComparison: {
        pass: region3dFloorPass,
        rows: region3dFloorComparison,
      },
      s1: result.region3dFloor.s1,
      s2: result.region3dFloor.s2,
      s3: result.region3dFloor.s3,
      s4: result.region3dFloor.s4,
      s5: result.region3dFloor.s5,
    },
    append: {
      requested:
        appendImageGoldens ||
        appendPathGoldens ||
        appendConnectorGoldens ||
        appendChromeGoldens ||
        appendW4Goldens ||
        appendT2Goldens ||
        appendRegion3dGoldens ||
        amendRegion3dGizmoGolden ||
        appendImageInputAmendment ||
        appendPathInputAmendment ||
        appendConnectorInputAmendment ||
        appendChromeInputAmendment ||
        appendW4InputAmendment ||
        appendT2InputAmendment ||
        amendShaderDigests,
      kind: appendImageGoldens
        ? "image"
        : appendPathGoldens
          ? "path"
          : appendConnectorGoldens
            ? "connector"
          : appendChromeGoldens
            ? "chrome"
          : appendW4Goldens
            ? "w4-frame-runtime"
          : appendT2Goldens
            ? "t2-input-floor"
          : appendRegion3dGoldens
            ? "region3d-floor"
          : amendRegion3dGizmoGolden
            ? "region3d-gizmo-golden-amendment"
          : amendShaderDigests
            ? "shader-digest-amendment"
          : appendImageInputAmendment
            ? "image-input-amendment"
          : appendPathInputAmendment
            ? "path-input-amendment"
            : appendConnectorInputAmendment
              ? "connector-input-amendment"
            : appendChromeInputAmendment
              ? "chrome-input-amendment"
            : appendW4InputAmendment
              ? "w4-input-amendment"
            : appendT2InputAmendment
              ? "t2-input-amendment"
            : null,
      preflight: appendImageGoldens
        ? appendPreflight
        : appendPathGoldens
          ? pathAppendPreflight
          : appendConnectorGoldens
            ? connectorAppendPreflight
          : appendChromeGoldens
            ? chromeAppendPreflight
          : appendW4Goldens
            ? w4AppendPreflight
          : appendT2Goldens
            ? t2AppendPreflight
          : appendRegion3dGoldens
            ? region3dAppendPreflight
          : amendRegion3dGizmoGolden
            ? region3dGizmoAmendmentPreflight
          : amendT2LivedCorrection
            ? t2LivedCorrectionPreflight
          : amendShaderDigests
            ? shaderAmendmentPreflight
          : appendImageInputAmendment
            ? inputAmendmentPreflight
          : appendW4InputAmendment
            ? w4InputAmendmentPreflight
          : appendT2InputAmendment
            ? t2InputAmendmentPreflight
          : appendChromeInputAmendment
            ? chromeInputAmendmentPreflight
          : appendConnectorInputAmendment
              ? connectorInputAmendmentPreflight
              : inputAmendmentPreflight,
      authorized: appendImageGoldens
        ? appendAuthorized
        : appendPathGoldens
          ? pathAppendAuthorized
          : appendConnectorGoldens
            ? connectorAppendAuthorized
          : appendChromeGoldens
            ? chromeAppendAuthorized
          : appendW4Goldens
            ? w4AppendAuthorized
          : appendT2Goldens
            ? t2AppendAuthorized
          : appendRegion3dGoldens
            ? region3dAppendAuthorized
          : amendRegion3dGizmoGolden
            ? region3dGizmoAmendmentAuthorized
          : amendT2LivedCorrection
            ? t2LivedCorrectionAuthorized
          : amendShaderDigests
            ? shaderAmendmentAuthorized
          : appendImageInputAmendment
            ? inputAmendmentAuthorized
          : appendPathInputAmendment
            ? inputAmendmentAuthorized
            : appendConnectorInputAmendment
              ? connectorInputAmendmentAuthorized
            : appendChromeInputAmendment
              ? chromeInputAmendmentAuthorized
            : appendW4InputAmendment
              ? w4InputAmendmentAuthorized
            : appendT2InputAmendment
              ? t2InputAmendmentAuthorized
          : false,
      postflightPass: appendPostflightPass,
    },
    assertion: {
      requested:
        assertImageContract ||
        assertPathContract ||
        assertConnectorContract ||
        assertChromeContract ||
        assertW4Contract ||
        assertT2Contract ||
        assertRegion3dContract,
      pass: assertionFailures.length === 0,
      failures: assertionFailures,
    },
    stdoutSummary,
    browserConsole,
    browserResult: stripDataUrls(result),
  };
  fs.mkdirSync(path.dirname(receiptFile), { recursive: true });
  fs.writeFileSync(receiptFile, `${JSON.stringify(receipt, null, 2)}\n`);

  console.log(JSON.stringify(stdoutSummary, null, 2));

  if (
    assertImageContract ||
    assertPathContract ||
    assertConnectorContract ||
    assertChromeContract ||
    assertW4Contract ||
    assertT2Contract ||
    assertRegion3dContract
  ) {
    if (assertionFailures.length > 0) process.exitCode = 1;
  } else if (
    !pass &&
    !(
      appendImageGoldens ||
      appendPathGoldens ||
      appendConnectorGoldens ||
      appendChromeGoldens ||
      appendW4Goldens ||
      appendT2Goldens ||
      appendRegion3dGoldens ||
      amendRegion3dGizmoGolden ||
      amendT2LivedCorrection ||
      appendImageInputAmendment ||
      appendPathInputAmendment ||
      appendConnectorInputAmendment ||
      appendChromeInputAmendment ||
      appendW4InputAmendment ||
      appendT2InputAmendment ||
      amendShaderDigests
    )
  ) {
    process.exitCode = 1;
  }
};

main().catch((error) => {
  console.error(error.stack || String(error));
  process.exitCode = 1;
});
