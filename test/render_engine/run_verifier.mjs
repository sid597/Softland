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
const origin = "https://softland-render-verifier.invalid";
const chromeExecutable =
  process.env.RENDER_VERIFIER_CHROME || "/usr/bin/google-chrome";
const updateGoldens = process.argv.includes("--update-goldens");
const appendImageGoldens = process.argv.includes("--append-image-goldens");
const appendPathGoldens = process.argv.includes("--append-path-goldens");
const appendConnectorGoldens = process.argv.includes(
  "--append-connector-goldens",
);
const appendPathInputAmendment = process.argv.includes(
  "--append-path-input-amendment",
);
const appendConnectorInputAmendment = process.argv.includes(
  "--append-connector-input-amendment",
);
const assertImageContract = process.argv.includes("--assert-image-contract");
const assertPathContract = process.argv.includes("--assert-path-contract");
const assertConnectorContract = process.argv.includes(
  "--assert-connector-contract",
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
      appendPathInputAmendment,
      appendConnectorInputAmendment,
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
    headless: true,
    ignoreHTTPSErrors: true,
    args: launchArgs,
  });
  const page = await browser.newPage();
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
  };
  const deterministic = determinismRows(result);
  const imageDeterministic = imageAtomDeterminismRows(result);
  const pathDeterministic = pathAtomDeterminismRows(result);
  const connectorDeterministic = connectorAtomDeterminismRows(result);
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
  const inputAmendmentPreflight = {
    bankPresent,
    sourceMatch,
    environmentMatch,
    legacyGoldensPass: imagePass,
    imageGoldensPass: imageAtomPass,
    pathGoldensPass: pathAtomPass,
    connectorGoldensPass: connectorAtomPass,
    legacyDeterminismPass: deterministic.length === 21 && determinismPass,
    imageDeterminismPass,
    pathDeterminismPass,
    connectorDeterminismPass,
    imageParityPass,
    pathParityPass,
    connectorParityPass,
    imageContractPass: result.imageAtom.pass,
    pathContractPass: result.pathAtom.pass,
    connectorContractPass: result.connectorAtom.pass,
    pathUploadGatePass: result.pathAtom.uploadGate?.pass,
    priorImageInputsMatch: imageAtomInputsMatch,
    priorPathInputsMatch: pathAtomInputsMatch,
    priorConnectorInputsMatch: connectorAtomInputsMatch,
  };
  const inputAmendmentAuthorized =
    bankPresent &&
    sourceMatch &&
    environmentMatch &&
    imagePass &&
    imageAtomPass &&
    pathAtomPass &&
    connectorAtomPass &&
    deterministic.length === 21 &&
    determinismPass &&
    imageDeterminismPass &&
    pathDeterminismPass &&
    connectorDeterminismPass &&
    imageParityPass &&
    pathParityPass &&
    connectorParityPass &&
    result.imageAtom.pass &&
    result.pathAtom.pass &&
    result.connectorAtom.pass &&
    result.pathAtom.uploadGate?.pass;

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
        connectorAtomInputsMatch));
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
    appendPathInputAmendment,
    inputAmendmentAuthorized,
    appendConnectorInputAmendment,
    connectorInputAmendmentAuthorized,
    assertImageContract,
    assertPathContract,
    assertConnectorContract,
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
      "clj -M:dev -m shadow.cljs.devtools.cli release render-verifier && node test/render_engine/run_verifier.mjs --append-connector-goldens --assert-connector-contract",
    pass,
    classification,
    bankPresent,
    updateRequested:
      appendImageGoldens ||
      appendPathGoldens ||
      appendConnectorGoldens ||
      appendPathInputAmendment ||
      appendConnectorInputAmendment,
    updateAuthorized: appendImageGoldens
      ? appendAuthorized
      : appendPathGoldens
        ? pathAppendAuthorized
        : appendConnectorGoldens
          ? connectorAppendAuthorized
        : appendPathInputAmendment
          ? inputAmendmentAuthorized
          : appendConnectorInputAmendment
            ? connectorInputAmendmentAuthorized
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
    append: {
      requested:
        appendImageGoldens ||
        appendPathGoldens ||
        appendConnectorGoldens ||
        appendPathInputAmendment ||
        appendConnectorInputAmendment,
      kind: appendImageGoldens
        ? "image"
        : appendPathGoldens
          ? "path"
          : appendConnectorGoldens
            ? "connector"
          : appendPathInputAmendment
            ? "path-input-amendment"
            : appendConnectorInputAmendment
              ? "connector-input-amendment"
            : null,
      preflight: appendImageGoldens
        ? appendPreflight
        : appendPathGoldens
          ? pathAppendPreflight
          : appendConnectorGoldens
            ? connectorAppendPreflight
            : appendConnectorInputAmendment
              ? connectorInputAmendmentPreflight
              : inputAmendmentPreflight,
      authorized: appendImageGoldens
        ? appendAuthorized
        : appendPathGoldens
          ? pathAppendAuthorized
          : appendConnectorGoldens
            ? connectorAppendAuthorized
          : appendPathInputAmendment
            ? inputAmendmentAuthorized
            : appendConnectorInputAmendment
              ? connectorInputAmendmentAuthorized
          : false,
      postflightPass: appendPostflightPass,
    },
    assertion: {
      requested:
        assertImageContract || assertPathContract || assertConnectorContract,
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

  if (assertImageContract || assertPathContract || assertConnectorContract) {
    if (assertionFailures.length > 0) process.exitCode = 1;
  } else if (!pass) {
    process.exitCode = 1;
  }
};

main().catch((error) => {
  console.error(error.stack || String(error));
  process.exitCode = 1;
});
