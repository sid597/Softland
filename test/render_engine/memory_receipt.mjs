#!/usr/bin/env node

import fs from "node:fs";
import http from "node:http";
import path from "node:path";
import process from "node:process";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

import puppeteer from "puppeteer";

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, "../..");
const publicRoot = path.join(repoRoot, "resources/public");
const outputFile = path.join(repoRoot, "target/layout-retention/memory-receipt.json");
const chromeExecutable =
  process.env.LAYOUT_RETENTION_CHROME || "/usr/bin/google-chrome";
const skipBuild = process.argv.includes("--skip-build");
const mode = process.argv.includes("--dev") ? "dev" : "release";
const liveUrl = process.argv
  .find((arg) => arg.startsWith("--url="))
  ?.slice("--url=".length);
const settleMs = Number(
  process.argv.find((arg) => arg.startsWith("--settle-ms="))?.split("=")[1] ||
    30_000,
);
const bootTimeoutMs = Number(
  process.argv.find((arg) => arg.startsWith("--boot-timeout-ms="))
    ?.split("=")[1] || 180_000,
);
const launchArgs = [
  "--no-sandbox",
  "--enable-unsafe-webgpu",
  "--enable-webgpu-developer-features",
  "--enable-features=Vulkan,WebGPU,UnsafeWebGPU",
  "--enable-precise-memory-info",
  "--js-flags=--expose-gc",
];

if (mode === "release" && !skipBuild) {
  // `prod` is also a deps.edn alias, so invoking the npm wrapper makes
  // shadow-cljs interpret it as a source namespace.  Use the repository's
  // established CLI lane (the same shape as verify:render-engine).
  execFileSync(
    "clj",
    ["-M:dev:prod", "-m", "shadow.cljs.devtools.cli", "release", "prod"],
    {
      cwd: repoRoot,
      stdio: "inherit",
    },
  );
}

const moduleOutputName = () => {
  const manifest = fs.readFileSync(path.join(publicRoot, "js/manifest.edn"), "utf8");
  const output = manifest.match(/:module-id :main[\s\S]*?:output-name "([^"]+)"/);
  if (!output) throw new Error("prod/dev manifest has no :main output-name");
  return output[1];
};

const mime = (file) => {
  if (file.endsWith(".html")) return "text/html; charset=utf-8";
  if (file.endsWith(".js")) return "text/javascript; charset=utf-8";
  if (file.endsWith(".json")) return "application/json; charset=utf-8";
  if (file.endsWith(".png")) return "image/png";
  if (file.endsWith(".css")) return "text/css; charset=utf-8";
  if (file.endsWith(".bin")) return "application/octet-stream";
  if (file.endsWith(".ttf")) return "font/ttf";
  if (file.endsWith(".woff2")) return "font/woff2";
  return "application/octet-stream";
};

const servePublic = async () => {
  const main = moduleOutputName();
  const server = http.createServer((request, response) => {
    try {
      const url = new URL(request.url, "http://127.0.0.1");
      const relative = decodeURIComponent(url.pathname === "/" ? "/index.html" : url.pathname);
      const file = path.resolve(publicRoot, `.${relative}`);
      if (file !== publicRoot && !file.startsWith(`${publicRoot}${path.sep}`)) {
        response.writeHead(403).end("forbidden");
        return;
      }
      if (!fs.existsSync(file) || !fs.statSync(file).isFile()) {
        response.writeHead(404).end("not found");
        return;
      }
      let body = fs.readFileSync(file);
      if (file.endsWith("index.html")) {
        body = Buffer.from(
          body
            .toString("utf8")
            .replace("$:hyperfiddle.client.module/main$", `/js/${main}`),
        );
      }
      response.writeHead(200, { "content-type": mime(file), "cache-control": "no-store" });
      response.end(body);
    } catch (error) {
      response.writeHead(500).end(String(error));
    }
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const address = server.address();
  return { server, url: `http://127.0.0.1:${address.port}/?live-atoms=1` };
};

const adapterIdentity = async (page) =>
  page.evaluate(async () => {
    if (!navigator.gpu) throw new Error("navigator.gpu unavailable");
    const adapter = await navigator.gpu.requestAdapter();
    if (!adapter) throw new Error("requestAdapter returned nil");
    const info = adapter.info ||
      (adapter.requestAdapterInfo ? await adapter.requestAdapterInfo() : {});
    return {
      vendor: info.vendor || "unknown",
      architecture: info.architecture || "unknown",
      device: info.device || "unknown",
      description: info.description || "unknown",
      isFallbackAdapter:
        typeof adapter.isFallbackAdapter === "boolean"
          ? adapter.isFallbackAdapter
          : null,
      features: [...adapter.features].sort(),
    };
  });

const collectGarbageTwice = async (session) => {
  await session.send("HeapProfiler.collectGarbage");
  await session.send("HeapProfiler.collectGarbage");
};

const processCensus = (rootPid) => {
  const output = execFileSync("ps", ["-eo", "pid=,ppid=,rss=,comm=,args="], {
    encoding: "utf8",
  });
  const rows = output
    .trim()
    .split("\n")
    .map((line) => {
      const match = line
        .trim()
        .match(/^(\d+)\s+(\d+)\s+(\d+)\s+(\S+)\s+(.+)$/);
      return match
        ? {
            pid: Number(match[1]),
            ppid: Number(match[2]),
            rssKb: Number(match[3]),
            command: match[4],
            args: match[5],
          }
        : null;
    })
    .filter(Boolean);
  const descendants = new Set([rootPid]);
  let changed = true;
  while (changed) {
    changed = false;
    for (const row of rows) {
      if (descendants.has(row.ppid) && !descendants.has(row.pid)) {
        descendants.add(row.pid);
        changed = true;
      }
    }
  }
  const tree = rows.filter((row) => descendants.has(row.pid));
  return { totalRssBytes: tree.reduce((sum, row) => sum + row.rssKb * 1024, 0), processes: tree };
};

const nonBlankCanvas = async (page) => {
  const canvas = await page.$("#webgpu-canvas");
  if (!canvas) return { pass: false, reason: "canvas-missing" };
  const screenshot = await canvas.screenshot({ type: "png" });
  const mean = Number(
    execFileSync(
      "identify",
      ["-format", "%[fx:mean]", "png:-"],
      { input: screenshot, encoding: "utf8" },
    ),
  );
  const dimensions = await canvas.evaluate((node) => [node.width, node.height]);
  return { pass: mean > 0, mean, width: dimensions[0], height: dimensions[1] };
};

const sample = async (page, session, browserPid) => {
  await collectGarbageTwice(session);
  await new Promise((resolve) => setTimeout(resolve, 500));
  const [metrics, runtimeHeapUsage, app] = await Promise.all([
    page.metrics(),
    session.send("Runtime.getHeapUsage"),
    page.evaluate(() => {
      const rawPlanes = window.__softlandLayoutRetention?.planeCensus?.();
      const planes = rawPlanes && {
        layoutCount: rawPlanes.layoutCount ?? rawPlanes["layout-count"],
        planeBytes: rawPlanes.planeBytes ?? rawPlanes["plane-bytes"],
        glyphBytes: rawPlanes.glyphBytes ?? rawPlanes["glyph-bytes"],
        spanBytes: rawPlanes.spanBytes ?? rawPlanes["span-bytes"],
        glyphOrderBytes:
          rawPlanes.glyphOrderBytes ?? rawPlanes["glyph-order-bytes"],
        glyphCount: rawPlanes.glyphCount ?? rawPlanes["glyph-count"],
        spanCount: rawPlanes.spanCount ?? rawPlanes["span-count"],
      };
      const slots = window.sceneFaces?.list?.();
      return {
        planes: planes || null,
        corpusFingerprint: {
          slotCount: Array.isArray(slots) ? slots.length : null,
          planeGlyphTotal: planes?.glyphCount ?? null,
        },
        frameCount: window.__layoutRetentionFrameCount || 0,
        schedulerReceipt: window.__softlandFrameSchedulerReceipt || null,
      };
    }),
  ]);
  return {
    usedJsHeapBytes: metrics.JSHeapUsedSize,
    totalJsHeapBytes: metrics.JSHeapTotalSize,
    runtimeHeapUsage,
    ...app,
    processCensus: processCensus(browserPid),
  };
};

let browser;
let server;
try {
  const served = liveUrl ? { url: liveUrl } : await servePublic();
  server = served.server;
  browser = await puppeteer.launch({
    executablePath: chromeExecutable,
    headless: "new",
    args: launchArgs,
    defaultViewport: { width: 1920, height: 1080, deviceScaleFactor: 1 },
  });
  const page = await browser.newPage();
  const gpuBudgetLogs = [];
  const bootDiagnostics = [];
  page.on("console", (message) => {
    const text = message.text();
    if (text.includes("[GPU-BUDGET]")) gpuBudgetLogs.push(text);
    if (["error", "warning"].includes(message.type())) {
      const details = message.args().map((arg) => {
        const remote = arg._remoteObject;
        return remote.value ?? remote.description ?? remote.className ?? remote.type;
      });
      bootDiagnostics.push({
        type: `console-${message.type()}`,
        text,
        details,
      });
      console.error(
        `[memory-receipt/${message.type()}] ${JSON.stringify(details)}`,
      );
    }
  });
  page.on("pageerror", (error) => {
    bootDiagnostics.push({ type: "pageerror", text: error.stack || String(error) });
    console.error(`[memory-receipt/pageerror] ${error.stack || error}`);
  });
  page.on("requestfailed", (request) => {
    const row = {
      type: "requestfailed",
      text: `${request.failure()?.errorText || "unknown"} ${request.url()}`,
    };
    bootDiagnostics.push(row);
    console.error(`[memory-receipt/requestfailed] ${row.text}`);
  });
  await page.evaluateOnNewDocument(() => {
    window.__layoutRetentionFrameCount = 0;
    const request = window.requestAnimationFrame.bind(window);
    window.requestAnimationFrame = (callback) =>
      request((time) => {
        window.__layoutRetentionFrameCount += 1;
        callback(time);
      });
  });
  const session = await page.target().createCDPSession();
  await session.send("HeapProfiler.enable");
  await page.goto(served.url, { waitUntil: "domcontentloaded", timeout: 120_000 });

  const adapter = await adapterIdentity(page);
  // Adapter identity is deliberately the first receipt line.
  console.log(JSON.stringify({ adapterIdentity: adapter }));

  try {
    await page.waitForFunction(
      () => {
        const planes = window.__softlandLayoutRetention?.planeCensus?.();
        return (planes?.glyphCount || planes?.["glyph-count"] || 0) > 0;
      },
      { timeout: bootTimeoutMs },
    );
  } catch (error) {
    const state = await page.evaluate(() => ({
      url: location.href,
      title: document.title,
      readyState: document.readyState,
      bodyText: document.body?.innerText?.slice(0, 2000) || "",
      frameCount: window.__layoutRetentionFrameCount || 0,
      layoutRetentionInstalled: Boolean(window.__softlandLayoutRetention),
      planes: window.__softlandLayoutRetention?.planeCensus?.() || null,
    }));
    fs.mkdirSync(path.dirname(outputFile), { recursive: true });
    fs.writeFileSync(
      path.join(path.dirname(outputFile), "boot-diagnostic.json"),
      `${JSON.stringify({ state, bootDiagnostics }, null, 2)}\n`,
    );
    console.error(JSON.stringify({ bootDiagnostic: state }));
    throw error;
  }
  await page.waitForFunction(
    () => (window.__layoutRetentionFrameCount || 0) >= 60,
    { timeout: bootTimeoutMs },
  );
  await new Promise((resolve) => setTimeout(resolve, settleMs));

  const visual = await nonBlankCanvas(page);
  const samples = [];
  for (let i = 0; i < 3; i += 1) {
    samples.push(await sample(page, session, browser.process().pid));
  }
  const maxHeap = Math.max(...samples.map((row) => row.usedJsHeapBytes));
  const maxPlanes = Math.max(...samples.map((row) => row.planes?.planeBytes || Infinity));
  const fingerprintStable = samples.every(
    (row) =>
      JSON.stringify(row.corpusFingerprint) === JSON.stringify(samples[0].corpusFingerprint),
  );
  const receipt = {
    mode,
    adapterIdentity: adapter,
    launch: { chromeExecutable, launchArgs, viewport: [1920, 1080], settleMs },
    corpusFingerprint: samples[0].corpusFingerprint,
    fingerprintStable,
    visual,
    gpuBudgetLogs,
    samples,
    gates: {
      releaseHeap100Mb: mode !== "release" || maxHeap <= 100_000_000,
      planes15Mb: maxPlanes <= 15_000_000,
      frames60AndNonBlank: samples[0].frameCount >= 60 && visual.pass,
      sameSampleFingerprint: fingerprintStable,
    },
  };
  fs.mkdirSync(path.dirname(outputFile), { recursive: true });
  fs.writeFileSync(outputFile, `${JSON.stringify(receipt, null, 2)}\n`);
  console.log(JSON.stringify({ receipt: path.relative(repoRoot, outputFile), gates: receipt.gates }));
  if (Object.values(receipt.gates).some((pass) => !pass)) process.exitCode = 1;
} finally {
  if (browser) await browser.close();
  if (server) await new Promise((resolve) => server.close(resolve));
}
