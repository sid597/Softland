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

const writeActualImages = (result) => {
  fs.mkdirSync(actualDir, { recursive: true });
  for (const renderCase of result.cases) {
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

const expectedDivergenceRows = (result) =>
  result.cases.map((renderCase) => ({
    caseId: renderCase.caseId,
    zoom: renderCase.zoom,
    regime: renderCase.regime,
    ...renderCase.currentProductPickSentinel,
  }));

const main = async () => {
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
      adapter: result.adapter,
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
  };
  const deterministic = determinismRows(result);
  const parity = parityRows(result);
  const divergences = expectedDivergenceRows(result);
  const determinismPass = deterministic.every((row) => row.byteIdentical);
  const parityPass = parity.every((row) => row.pass);
  const divergencePass = divergences.every((row) => row.expectedDivergence);
  // Pixel-bank custody and geometry-contract parity are independent receipts.
  // A deterministic production image remains worth banking when candidate
  // point-in-path parity is red; the replay must still exit red below and must
  // never turn that disagreement into an accepted geometry baseline.
  const updateAuthorized = determinismPass && divergencePass;

  Object.assign(currentManifest, {
    replayCommand: "npm run verify:render-engine",
    environmentFingerprintSha256: environment.fingerprintSha256,
    baselineReceipts: {
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

  if (updateGoldens && updateAuthorized) {
    fs.mkdirSync(goldenDir, { recursive: true });
    for (const renderCase of result.cases) {
      for (const image of renderCase.images) {
        const png = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
        fs.writeFileSync(path.join(goldenDir, image.file), png);
      }
    }
    fs.writeFileSync(
      manifestFile,
      `${JSON.stringify(currentManifest, null, 2)}\n`,
    );
    fs.writeFileSync(
      environmentFile,
      `${JSON.stringify(environment, null, 2)}\n`,
    );
  }

  const bankPresent =
    fs.existsSync(manifestFile) && fs.existsSync(environmentFile);
  const expectedManifest = bankPresent ? jsonRead(manifestFile) : null;
  const expectedEnvironment = bankPresent ? jsonRead(environmentFile) : null;
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
  const environmentMatch =
    bankPresent &&
    expectedEnvironment.fingerprintSha256 === environment.fingerprintSha256;
  const expectedByFile = new Map(
    (expectedManifest?.images || []).map((image) => [image.file, image]),
  );
  const imageComparison = currentManifest.images.map((image) => {
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
  const imagePass =
    bankPresent &&
    imageComparison.length === (expectedManifest?.images.length || 0) &&
    imageComparison.every(
      (row) => row.rawMatch && row.pngManifestMatch && row.goldenFileMatch,
    );

  let classification = "pass";
  if (!determinismPass) classification = "determinism-failure";
  else if (!parityPass) classification = "candidate-pick-parity-failure";
  else if (!divergencePass)
    classification = "current-product-pick-sentinel-failure";
  else if (!bankPresent) classification = "missing-golden-bank";
  else if (!sourceMatch) classification = "production-source-mismatch";
  else if (!environmentMatch) classification = "environment-mismatch";
  else if (!imagePass) classification = "same-environment-pixel-drift";

  const pass = classification === "pass";
  const receipt = {
    schemaVersion: 1,
    verifier: result.verifier,
    replayCommand: "npm run verify:render-engine",
    updateCommand:
      "clj -M:dev -m shadow.cljs.devtools.cli release render-verifier && node test/render_engine/run_verifier.mjs --update-goldens",
    pass,
    classification,
    bankPresent,
    updateRequested: updateGoldens,
    updateAuthorized,
    updateAuthority:
      "golden-bank update requires deterministic pixels and the current-product divergence sentinel; candidate parity remains independently gating and cannot be blessed by an image update",
    sourceMatch,
    environmentMatch,
    environment,
    currentManifest,
    determinism: {
      pass: determinismPass,
      rows: deterministic,
    },
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
    browserConsole,
    browserResult: stripDataUrls(result),
  };
  fs.mkdirSync(path.dirname(receiptFile), { recursive: true });
  fs.writeFileSync(receiptFile, `${JSON.stringify(receipt, null, 2)}\n`);

  console.log(
    JSON.stringify(
      {
        pass,
        classification,
        receipt: path.relative(repoRoot, receiptFile),
        images: currentManifest.images.length,
        deterministic: `${deterministic.filter((row) => row.byteIdentical).length}/${deterministic.length}`,
        candidateParity: `${parity.filter((row) => row.pass).length}/${parity.length}`,
        productBoundsDivergenceSentinels: `${divergences.filter((row) => row.expectedDivergence).length}/${divergences.length}`,
        updateRequested: updateGoldens,
        updateAuthorized,
        environmentFingerprint: environment.fingerprintSha256,
      },
      null,
      2,
    ),
  );

  if (!pass) process.exitCode = 1;
};

main().catch((error) => {
  console.error(error.stack || String(error));
  process.exitCode = 1;
});
