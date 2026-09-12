#!/usr/bin/env node
// The shaper WASM-border receipt runner (history/docs/shaping-correction, 2026-08-30).
// Drives the :shaper-border-probe shadow build in HEADFUL Chrome on the proven
// desktop road (DISPLAY :0 + Vulkan ANGLE → the physical adapter) so the
// receipt opens with a real adapter attestation, then records the ladder.
//
//   node test/render_engine/profile_shaper_border.mjs --corpus=<corpus.json> [--out=<dir>]
//
// Build first: clj -M:dev -m shadow.cljs.devtools.cli release shaper-border-probe

import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import process from "node:process";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

import puppeteer from "puppeteer";

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, "../..");
const buildFile = path.join(repoRoot, "target/shaper-border-probe/js/main.js");
const origin = "http://localhost";
const chromeExecutable =
  process.env.RENDER_VERIFIER_CHROME || "/usr/bin/google-chrome";
// The proven headful road (memory reference-desktop-browser-harness-road,
// 2026-08-09) + precise memory + exposed gc for cleaner rung brackets.
const launchArgs = [
  "--no-sandbox",
  "--enable-unsafe-webgpu",
  "--use-angle=vulkan",
  "--enable-features=Vulkan,WebGPU,UnsafeWebGPU",
  "--ignore-gpu-blocklist",
  "--enable-precise-memory-info",
  "--js-flags=--expose-gc",
  "--window-size=800,601",
];

const args = Object.fromEntries(
  process.argv.slice(2).map((arg) => {
    const m = /^--([^=]+)=(.*)$/.exec(arg);
    if (!m) throw new Error(`bad arg ${arg}`);
    return [m[1], m[2]];
  }),
);
if (!args.corpus) throw new Error("--corpus=<corpus.json> is required");
const corpusFile = path.resolve(args.corpus);
const outDir = path.resolve(args.out || path.join(repoRoot, "target/shaper-border-probe"));

const sha256 = (bytes) => crypto.createHash("sha256").update(bytes).digest("hex");
const sha256File = (absolutePath) => ({
  path: path.relative(repoRoot, absolutePath),
  bytes: fs.statSync(absolutePath).size,
  sha256: sha256(fs.readFileSync(absolutePath)),
});

const mimeType = (p) =>
  p.endsWith(".js") ? "text/javascript; charset=utf-8"
  : p.endsWith(".json") ? "application/json"
  : "application/octet-stream";

const html = `<!doctype html>
<html><head><meta charset="utf-8"><title>Softland shaper border probe</title></head>
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
      if (url.pathname === "/corpus.json") {
        return request.respond({ status: 200, contentType: "application/json", body: fs.readFileSync(corpusFile) });
      }
      if (url.pathname.startsWith("/fonts/")) {
        const relative = decodeURIComponent(url.pathname.slice(1));
        const absolute = path.resolve(repoRoot, "resources/public", relative);
        const fontRoot = path.resolve(repoRoot, "resources/public/fonts");
        if (!absolute.startsWith(`${fontRoot}${path.sep}`)) return request.abort();
        return request.respond({ status: 200, contentType: mimeType(absolute), body: fs.readFileSync(absolute) });
      }
      return request.respond({ status: 404, body: "not found" });
    } catch (_error) {
      request.abort();
    }
  });
};

const cpuModel = () => {
  try {
    const line = fs.readFileSync("/proc/cpuinfo", "utf8").split("\n").find((l) => l.startsWith("model name"));
    return line ? line.split(":")[1].trim() : os.cpus()[0]?.model;
  } catch { return os.cpus()[0]?.model; }
};
const gitHead = () => {
  try { return execFileSync("git", ["rev-parse", "HEAD"], { cwd: repoRoot }).toString().trim(); }
  catch { return null; }
};
const gitDirty = () => {
  try { return execFileSync("git", ["status", "--porcelain"], { cwd: repoRoot }).toString().trim().split("\n").filter(Boolean); }
  catch { return null; }
};

const main = async () => {
  if (!fs.existsSync(buildFile)) throw new Error(`Missing probe build ${path.relative(repoRoot, buildFile)}`);
  if (!fs.existsSync(corpusFile)) throw new Error(`Missing corpus ${corpusFile}`);
  const startedAt = new Date().toISOString();
  const browser = await puppeteer.launch({
    executablePath: chromeExecutable,
    headless: false,
    ignoreHTTPSErrors: true,
    args: launchArgs,
    env: { ...process.env, DISPLAY: process.env.DISPLAY || ":0" },
  });
  const consoleLines = [];
  try {
    const page = await browser.newPage();
    await page.setViewport({ width: 800, height: 601, deviceScaleFactor: 1.046875 });
    page.on("console", (message) => {
      const row = { type: message.type(), text: message.text() };
      consoleLines.push(row);
      if (row.text.startsWith("[BORDER]")) console.log(row.text);
    });
    page.on("pageerror", (error) => consoleLines.push({ type: "pageerror", text: String(error) }));
    await serveSyntheticOrigin(page);
    await page.goto(origin, { waitUntil: "load", timeout: 30_000 });
    await page.waitForFunction(() => window.__shaperBorderDone === true, { timeout: 600_000 });
    const result = await page.evaluate(() => window.__shaperBorderResult);
    const chromeVersion = await browser.version();
    if (!result || result.fatal) {
      throw new Error(`probe failed: ${result?.fatal || "missing result"}\n${result?.stack || ""}`);
    }
    const receipt = {
      schemaVersion: 1,
      question: "shaping-correction · shaper WASM border: ms at the border (getGlyphInfosAndPositions→js->clj per-glyph maps; per-glyph glyphExtents round trip) versus HarfBuzz's own shape() at the founding corpus",
      // Attestation FIRST — the contract's standing rule (CONTRACT §3/§9/§10).
      adapter: result.adapter,
      capture: {
        startedAt,
        finishedAt: new Date().toISOString(),
        hostname: os.hostname(),
        cpu: cpuModel(),
        cores: os.cpus().length,
        os: `${os.type()} ${os.release()}`,
        chrome: chromeVersion,
        chromeExecutable,
        puppeteer: JSON.parse(fs.readFileSync(path.join(repoRoot, "node_modules/puppeteer/package.json"), "utf8")).version,
        node: process.version,
        display: process.env.DISPLAY || ":0",
        headless: false,
        launchArgs,
        viewport: { width: 800, height: 601, deviceScaleFactor: 1.046875 },
        gitHead: gitHead(),
        gitDirty: gitDirty(),
        build: sha256File(buildFile),
        corpus: { ...sha256File(corpusFile), path: corpusFile },
        sources: [
          "src/app/client/text/shaper.cljs",
          "src/app/client/text/layout.cljc",
          "test/app/client/harness/shaper_border_probe.cljs",
          "test/render_engine/profile_shaper_border.mjs",
          "node_modules/harfbuzzjs/hbjs.js",
          "resources/public/fonts/harfbuzz-0.10.3.wasm",
          "resources/public/fonts/ubuntu_sans_variable.ttf",
          "resources/public/fonts/noto_sans_regular.ttf",
        ].map((p) => sha256File(path.join(repoRoot, p))),
      },
      environment: result.environment,
      provider: result.provider,
      corpus: result.corpus,
      passes: result.passes,
      layout: result.layout,
      ladder: result.ladder,
      console: consoleLines,
    };
    fs.mkdirSync(outDir, { recursive: true });
    const receiptFile = path.join(outDir, "receipt.json");
    fs.writeFileSync(receiptFile, `${JSON.stringify(receipt, null, 2)}\n`);
    console.log(JSON.stringify({
      receipt: receiptFile,
      receiptSha256: sha256(fs.readFileSync(receiptFile)),
      adapter: receipt.adapter,
      corpus: receipt.corpus,
      ladder: receipt.ladder.map(({ id, items, count, medianMs, median_ms, ...r }) => ({
        id, items, count, medianMs: r["median-ms"] ?? medianMs ?? median_ms, passMs: r["pass-ms"],
      })),
    }, null, 2));
  } finally {
    await browser.close();
  }
};

main().catch((error) => {
  console.error(error.stack || String(error));
  process.exitCode = 1;
});
