#!/usr/bin/env node

// Runs the render verifier's browser page once and writes the full harness
// result for the path and region3d lanes to a directory: the scenario
// numbers as JSON and every rendered picture as a PNG. run_verifier.mjs
// keeps only pass/fail and golden hashes in its receipt; this is how a judge
// gets every displayed number from the client itself.
//
//   clj -Sdeps '{:deps {thheller/shadow-cljs {:mvn/version "2.28.23"}}}' \
//     -M -m shadow.cljs.devtools.cli release render-verifier
//   node test/render_engine/dump_result.mjs <out-dir>
//
// Writes <out-dir>/path-step.json, <out-dir>/region3d-floor.json,
// <out-dir>/shader-digests.json and <out-dir>/png/<file>.png for each case.

import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";

import puppeteer from "puppeteer";

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, "../..");
const buildFile = path.join(repoRoot, "target/render-verifier/js/main.js");
const origin = "http://localhost";
const chromeExecutable =
  process.env.RENDER_VERIFIER_CHROME || "/usr/bin/google-chrome";
const launchArgs = [
  "--no-sandbox",
  "--enable-unsafe-webgpu",
  "--use-angle=swiftshader",
  "--enable-features=WebGPU,UnsafeWebGPU",
];
const html = `<!doctype html>
<html><head><meta charset="utf-8"><title>Softland parked renderer</title></head>
<body><script src="/js/main.js"></script></body></html>`;

const serveSyntheticOrigin = async (page) => {
  await page.setRequestInterception(true);
  page.on("request", (request) => {
    try {
      const url = new URL(request.url());
      if (url.origin !== origin) return request.abort();
      if (url.pathname === "/" || url.pathname === "/index.html") {
        return request.respond({ status: 200, contentType: "text/html; charset=utf-8", body: html });
      }
      if (url.pathname === "/js/main.js") {
        return request.respond({ status: 200, contentType: "text/javascript; charset=utf-8", body: fs.readFileSync(buildFile) });
      }
      if (url.pathname.startsWith("/fonts/")) {
        const absolute = path.resolve(repoRoot, "resources/public", decodeURIComponent(url.pathname.slice(1)));
        const fontRoot = path.resolve(repoRoot, "resources/public/fonts");
        if (!absolute.startsWith(`${fontRoot}${path.sep}`)) return request.abort();
        return request.respond({ status: 200, contentType: "application/octet-stream", body: fs.readFileSync(absolute) });
      }
      if (url.pathname.startsWith("/images/")) {
        const imageRoot = path.resolve(repoRoot, "test/app/fixtures/render_engine/images");
        const absolute = path.resolve(imageRoot, decodeURIComponent(url.pathname.slice("/images/".length)));
        if (!absolute.startsWith(`${imageRoot}${path.sep}`)) return request.abort();
        return request.respond({ status: 200, contentType: "image/png", body: fs.readFileSync(absolute) });
      }
      request.respond({ status: 404, body: "not found" });
    } catch (_error) {
      request.abort();
    }
  });
};

// Pulls every png-data-url out of a lane's result into <out>/png and
// replaces it in the JSON with the file's relative path.
const extractPngs = (value, pngDir, written) => {
  if (Array.isArray(value)) return value.map((child) => extractPngs(child, pngDir, written));
  if (value && typeof value === "object") {
    const out = {};
    for (const [key, child] of Object.entries(value)) {
      if (key === "png-data-url" && typeof child === "string" && value.file) {
        const bytes = Buffer.from(child.slice(child.indexOf(",") + 1), "base64");
        fs.writeFileSync(path.join(pngDir, value.file), bytes);
        written.push(value.file);
        out.png = path.join("png", value.file);
      } else {
        out[key] = extractPngs(child, pngDir, written);
      }
    }
    return out;
  }
  return value;
};

const main = async () => {
  const outDir = process.argv[2];
  if (!outDir) throw new Error("usage: dump_result.mjs <out-dir>");
  if (!fs.existsSync(buildFile)) {
    throw new Error(`Missing harness build ${path.relative(repoRoot, buildFile)}`);
  }
  const pngDir = path.join(outDir, "png");
  fs.mkdirSync(pngDir, { recursive: true });

  const browser = await puppeteer.launch({
    executablePath: chromeExecutable,
    headless: "chrome",
    ignoreHTTPSErrors: true,
    args: launchArgs,
  });
  let result;
  try {
    const page = await browser.newPage();
    page.on("console", (message) => {
      const text = message.text();
      if (text.startsWith("[W0-A]")) console.log(text);
    });
    await serveSyntheticOrigin(page);
    await page.goto(origin, { waitUntil: "load", timeout: 30_000 });
    await page.waitForFunction(() => window.__renderVerifierDone === true, { timeout: 600_000 });
    result = await page.evaluate(() => window.__renderVerifierResult);
  } finally {
    await browser.close();
  }
  if (!result || result.fatal) {
    throw new Error(`Browser harness failed: ${result?.fatal || "missing result"}`);
  }

  const written = [];
  const lanes = {
    "path-step": result["path-step"],
    "region3d-floor": result["region3d-floor"],
  };
  for (const [name, lane] of Object.entries(lanes)) {
    const cleaned = extractPngs(lane, pngDir, written);
    fs.writeFileSync(path.join(outDir, `${name}.json`), `${JSON.stringify(cleaned, null, 1)}\n`);
  }
  fs.writeFileSync(
    path.join(outDir, "shader-digests.json"),
    `${JSON.stringify({ harness: result.harness, adapter: result.adapter, "shader-digests": result["shader-digests"] }, null, 1)}\n`,
  );
  console.log(
    JSON.stringify({
      out: outDir,
      "path-step pass": lanes["path-step"]?.["pass?"],
      "region3d-floor pass": lanes["region3d-floor"]?.["pass?"],
      pngs: written,
    }, null, 1),
  );
};

main().catch((error) => {
  console.error(error.stack || String(error));
  process.exitCode = 1;
});
