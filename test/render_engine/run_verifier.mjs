#!/usr/bin/env node

import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";

import puppeteer from "puppeteer";

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, "../..");
const buildFile = path.join(repoRoot, "target/render-verifier/js/main.js");
const receiptFile = path.join(repoRoot, "target/render-verifier/receipt.json");
const goldenDir = path.join(
  repoRoot,
  "test/app/fixtures/render_engine/gpu-goldens",
);
const manifestFile = path.join(goldenDir, "manifest.json");
const origin = "http://localhost";
const chromeExecutable =
  process.env.RENDER_VERIFIER_CHROME || "/usr/bin/google-chrome";
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

const mimeType = (requestPath) => {
  if (requestPath.endsWith(".js")) return "text/javascript; charset=utf-8";
  if (requestPath.endsWith(".json")) return "application/json";
  if (requestPath.endsWith(".png")) return "image/png";
  if (requestPath.endsWith(".bin")) return "application/octet-stream";
  return "application/octet-stream";
};

const html = `<!doctype html>
<html><head><meta charset="utf-8"><title>Softland parked renderer</title></head>
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
    } catch (_error) {
      request.abort();
    }
  });
};

const imageRows = (cases = []) =>
  cases.flatMap((renderCase) =>
    (renderCase.images || []).map((image) => ({
      caseId: renderCase.caseId,
      mode: image.mode,
      file: image.file,
      rawSha256: image.rawSha256,
      pngSha256: sha256(
        Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64"),
      ),
      byteIdentical: image.determinism?.byteIdentical === true,
    })),
  );

const allDeterministic = (cases) => {
  const rows = imageRows(cases);
  return rows.length > 0 && rows.every((row) => row.byteIdentical);
};

const laneGuards = (result) => [
  {
    name: "base-renderer",
    pass:
      allDeterministic(result.cases) &&
      result.q8Transport?.pass === true &&
      result.q8Transport?.rows?.length === 3 &&
      result.q8Transport.rows.every((row) => row.pass === true),
  },
  {
    name: "ubuntu-slug",
    pass:
      result.ubuntuSlug?.pass === true &&
      allDeterministic(result.ubuntuSlug?.cases) &&
      result.ubuntuSlug?.faceIds?.join(",") ===
        "noto-sans-regular,ubuntu-sans-variable",
  },
  {
    name: "image",
    pass:
      result.imageStep?.pass === true &&
      allDeterministic(result.imageStep?.cases) &&
      result.imageStep?.parity?.every((row) => row.pass === true),
  },
  {
    name: "path",
    pass:
      result.pathStep?.pass === true &&
      allDeterministic(result.pathStep?.cases) &&
      result.pathStep?.parity?.every((row) => row.pass === true),
  },
  {
    name: "region3d",
    pass:
      result.region3dFloor?.pass === true &&
      allDeterministic(result.region3dFloor?.cases),
  },
  {
    // SHAPER-BORDER.md §8: F1 (flat shaper ≡ oracle shaper), F2 (flat layout
    // ≡ oracle layout, real HarfBuzz), F3 (flat pack bytes ≡ oracle bytes).
    // The µs/glyph bracket rides in the same block and is never gated.
    name: "text-flat-route",
    pass:
      result.textFlatRoute?.pass === true &&
      Array.isArray(result.textFlatRoute?.rows) &&
      result.textFlatRoute.rows.length >= 3 &&
      result.textFlatRoute.rows.every((row) => row.pass === true),
  },
];

const representativeSpecs = [
  { manifestKey: "images", resultKey: "cases", file: "gpu-slug-legal-min-z0p01.png" },
  {
    manifestKey: "images",
    resultKey: "ubuntuSlug",
    file: "gpu-slug-ubuntu-mixed-face.png",
  },
  {
    manifestKey: "pathAtomCases",
    resultKey: "pathStep",
    file: "gpu-path-holed-concave-holed-concave-default-unit-z1.png",
  },
  {
    manifestKey: "pathAtomCases",
    resultKey: "pathStep",
    file: "gpu-path-translucent-self-crossing-translucent-self-crossing-legal-z10.png",
  },
  {
    manifestKey: "pathAtomCases",
    resultKey: "pathStep",
    file: "gpu-path-container-tree-tree-containers-cid17-slot1.png",
  },
  {
    manifestKey: "region3dFloorCases",
    resultKey: "region3dFloor",
    file: "gpu-region3d-floor-tree.png",
  },
  {
    manifestKey: "images",
    resultKey: "textGroupTree",
    file: "gpu-slug-container-tree-mixed-face-cid17-slot1.png",
  },
];

const representativeGoldens = (result, manifest) =>
  representativeSpecs.map(({ manifestKey, resultKey, file }) => {
    const cases =
      resultKey === "cases"
        ? result.cases
        : resultKey === "textGroupTree"
          ? [result.ubuntuSlug?.groupTree?.case].filter(Boolean)
          : result[resultKey]?.cases;
    const current = imageRows(cases).find((row) => row.file === file);
    const expected = manifest[manifestKey]?.find((row) => row.file === file);
    const goldenFile = path.join(goldenDir, file);
    const goldenPngSha256 = fs.existsSync(goldenFile)
      ? sha256(fs.readFileSync(goldenFile))
      : null;
    const pass = Boolean(
      current &&
        expected &&
        current.rawSha256 === expected.rawSha256 &&
        current.pngSha256 === expected.pngSha256 &&
        goldenPngSha256 === expected.pngSha256,
    );
    return {
      family: resultKey,
      file,
      pass,
      currentRawSha256: current?.rawSha256 || null,
      expectedRawSha256: expected?.rawSha256 || null,
      currentPngSha256: current?.pngSha256 || null,
      expectedPngSha256: expected?.pngSha256 || null,
      goldenPngSha256,
    };
  });

const slugGoldens = (result, manifest) =>
  imageRows(result.cases).map((current) => {
    const expected = manifest.images?.find((row) => row.file === current.file);
    const goldenFile = path.join(goldenDir, current.file);
    const goldenPngSha256 = fs.existsSync(goldenFile)
      ? sha256(fs.readFileSync(goldenFile))
      : null;
    return {
      file: current.file,
      pass: Boolean(
        expected &&
          current.rawSha256 === expected.rawSha256 &&
          current.pngSha256 === expected.pngSha256 &&
          goldenPngSha256 === expected.pngSha256,
      ),
    };
  });

const filesUnder = (relativeDirectory, extensions) => {
  const walk = (directory) =>
    fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
      const absolutePath = path.join(directory, entry.name);
      return entry.isDirectory() ? walk(absolutePath) : [absolutePath];
    });

  return walk(path.join(repoRoot, relativeDirectory))
    .filter((absolutePath) => extensions.has(path.extname(absolutePath)))
    .map((absolutePath) =>
      path.relative(repoRoot, absolutePath).split(path.sep).join("/"),
    );
};

const sourceInputs = () =>
  [
    ...filesUnder("src/app/client", new Set([".cljs", ".cljc"])),
    ...filesUnder("test/render_engine", new Set([".mjs"])),
  ]
    .sort()
    .map(sha256File);

const recordUbuntuSlugGolden = (result, manifest) => {
  const cases = [
    ...(result.ubuntuSlug?.cases || []),
    result.ubuntuSlug?.groupTree?.case,
  ].filter(Boolean);
  const images = cases.flatMap((renderCase) =>
    renderCase.images.map((image) => ({ renderCase, image })),
  );
  if (images.length !== 2) {
    throw new Error(`Expected two Ubuntu Slug goldens, got ${images.length}`);
  }
  const rows = images.map(({ renderCase, image }) => {
    const bytes = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
    fs.writeFileSync(path.join(goldenDir, image.file), bytes);
    return {
      caseId: renderCase.caseId,
      zoom: renderCase.zoom,
      regime: renderCase.lod,
      normalization: renderCase.normalization,
      shapeExtentWorld: renderCase.shapeExtentWorld,
      mode: image.mode,
      file: image.file,
      rawSha256: image.rawSha256,
      pngSha256: sha256(bytes),
    };
  });
  const recordedFiles = new Set(rows.map((row) => row.file));
  manifest.images = [
    ...(manifest.images || []).filter((entry) => !recordedFiles.has(entry.file)),
    ...rows,
  ];
  fs.writeFileSync(manifestFile, `${JSON.stringify(manifest, null, 2)}\n`);
  return rows;
};

const recordPathGolden = (result, manifest, file) => {
  const matches = (result.pathStep?.cases || []).flatMap((renderCase) =>
    (renderCase.images || [])
      .filter((image) => image.file === file)
      .map((image) => ({ renderCase, image })),
  );
  if (matches.length !== 1) {
    throw new Error(`Expected one path golden named ${file}, got ${matches.length}`);
  }
  const { renderCase, image } = matches[0];
  const bytes = Buffer.from(image.pngDataUrl.split(",", 2)[1], "base64");
  fs.writeFileSync(path.join(goldenDir, file), bytes);
  const row = {
    caseId: renderCase.caseId,
    zoom: renderCase.zoom,
    regime: renderCase.lod,
    normalization: renderCase.normalization,
    shapeExtentWorld: renderCase.shapeExtentWorld,
    mode: image.mode,
    file,
    rawSha256: image.rawSha256,
    pngSha256: sha256(bytes),
  };
  manifest.pathAtomCases = [
    ...(manifest.pathAtomCases || []).filter((entry) => entry.file !== file),
    row,
  ];
  fs.writeFileSync(manifestFile, `${JSON.stringify(manifest, null, 2)}\n`);
  return row;
};

const runBrowser = async () => {
  const browser = await puppeteer.launch({
    executablePath: chromeExecutable,
    headless: "chrome",
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
    browserConsole.push({ type: "pageerror", text: String(error) });
  });
  try {
    await serveSyntheticOrigin(page);
    await page.goto(origin, { waitUntil: "load", timeout: 30_000 });
    await page.waitForFunction(() => window.__renderVerifierDone === true, {
      timeout: 600_000,
    });
    const result = camelizeKeys(
      await page.evaluate(() => window.__renderVerifierResult),
    );
    return { result, browserConsole };
  } finally {
    await browser.close();
  }
};

const main = async () => {
  if (process.argv.length !== 2) {
    throw new Error("The cleanup harness has one replay route: npm run verify:render-engine");
  }
  if (!fs.existsSync(buildFile)) {
    throw new Error(`Missing harness build ${path.relative(repoRoot, buildFile)}`);
  }
  if (!fs.existsSync(manifestFile)) {
    throw new Error("Missing parked renderer golden manifest");
  }

  const { result, browserConsole } = await runBrowser();
  if (!result || result.fatal) {
    throw new Error(
      `Browser harness failed: ${result?.fatal || "missing result"}\n${result?.stack || ""}`,
    );
  }

  const manifest = JSON.parse(fs.readFileSync(manifestFile, "utf8"));
  if (process.env.RENDER_VERIFIER_RECORD_UBUNTU_SLUG === "1") {
    recordUbuntuSlugGolden(result, manifest);
  }
  if (process.env.RENDER_VERIFIER_RECORD_PATH_TREE === "1") {
    recordPathGolden(
      result,
      manifest,
      "gpu-path-container-tree-tree-containers-cid17-slot1.png",
    );
  }
  const guards = laneGuards(result);
  const goldens = representativeGoldens(result, manifest);
  const dejavuSlugGoldens = slugGoldens(result, manifest);
  const pass =
    guards.every((guard) => guard.pass) &&
    goldens.every((row) => row.pass) &&
    dejavuSlugGoldens.length === 7 &&
    dejavuSlugGoldens.every((row) => row.pass);
  const receipt = {
    schemaVersion: 1,
    verifier: result.harness,
    authority: "parked scene, GPU, and render-family harness; no product client",
    replayCommand: "npm run verify:render-engine",
    pass,
    guards,
    dejavuSlugGoldens,
    ubuntuSlug: {
      text: result.ubuntuSlug?.text,
      faceIds: result.ubuntuSlug?.faceIds,
      faceRevisions: result.ubuntuSlug?.faceRevisions,
      pass: result.ubuntuSlug?.pass === true,
    },
    representativeGoldens: goldens,
    // SHAPER-BORDER.md §2(3): the flat route's consistency checks and its µs/glyph
    // bracket live in the receipt permanently (the bracket is never gated).
    textFlatRoute: result.textFlatRoute,
    sourceInputs: sourceInputs(),
    adapter: result.adapter,
    browserConsole,
  };
  fs.mkdirSync(path.dirname(receiptFile), { recursive: true });
  fs.writeFileSync(receiptFile, `${JSON.stringify(receipt, null, 2)}\n`);
  console.log(
    JSON.stringify({
      pass,
      guards,
      dejavuSlugGoldens,
      ubuntuSlug: receipt.ubuntuSlug,
      representativeGoldens: goldens.map(({ family, file, pass: rowPass }) => ({
        family,
        file,
        pass: rowPass,
      })),
      receipt: path.relative(repoRoot, receiptFile),
    }),
  );
  if (!pass) process.exitCode = 1;
};

main().catch((error) => {
  console.error(error.stack || String(error));
  process.exitCode = 1;
});
