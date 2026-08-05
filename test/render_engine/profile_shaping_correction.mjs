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
const chromeExecutable = "/usr/bin/google-chrome";
const defaultUrl = "http://localhost:8080";
const viewport = { width: 800, height: 601, deviceScaleFactor: 1.046875 };
// Sid's 2026-08-05 cadence ruling. This harness is retained as optional
// historical machinery; its long paired-page matrix is not an acceptance
// blocker and is not completed or repaired by the settlement pass.
const profilerSettlement = {
  status: "incomplete-retired-non-blocking",
  blockingAcceptancePath: false,
  all20RowsPassed: false,
  retainedPositiveRunStoppedAt: "g4-e-backend",
  retainedStopObservation: "backend flip observed zero slot-text writes before timeout",
  timingAdapter: "SwiftShader",
  physicalGpuTimingVerdict: "unclassified",
};
// The founding capture's 172 durable blocks were accompanied by five
// non-block Studio/Workshop playground slots.  That experiment was later
// discarded, while two intentionally blank Studio draft births made after the
// capture remain durable corpus truth.  Pin durable identity and slot
// ownership independently; never let one total conceal drift in the other.
const expectedCorpus = {
  blocks: 174,
  machineBlocks: 36,
  servedChars: 586927,
  blockIdDigest: "91d5245a6ea170b1f0f03923127a6e0f99bffdc8cb2e4521ba484fc9bbc56ec2",
  blockSlots: 174,
  liveTextSlots: 174,
  nonBlockSlots: 0,
};
const ordinaryId =
  "du:chat:77088a4f028100d3e93a99d291e2a184c7ed6ca66b3a998a74f9fbddfe62b34b:episode-native-v0:ep:3405ac6d:000000";
const largestId =
  "du:chat:77088a4f028100d3e93a99d291e2a184c7ed6ca66b3a998a74f9fbddfe62b34b:episode-native-v0:ep:3c6512e2:000000";
const launchArgs = [
  "--no-sandbox",
  "--enable-unsafe-webgpu",
  "--enable-webgpu-developer-features",
  "--enable-features=Vulkan,WebGPU,UnsafeWebGPU",
];

const sha256 = (bytes) =>
  crypto.createHash("sha256").update(bytes).digest("hex");

const stable = (value) => {
  if (Array.isArray(value)) return value.map(stable);
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.keys(value)
        .sort()
        .map((key) => [key, stable(value[key])]),
    );
  }
  return value;
};

const camelize = (value) => {
  if (Array.isArray(value)) return value.map(camelize);
  if (value && Object.getPrototypeOf(value) === Object.prototype) {
    return Object.fromEntries(
      Object.entries(value).map(([key, child]) => [
        key.replace(/-([a-z0-9])/g, (_, c) => c.toUpperCase()).replace(/\?$/, ""),
        camelize(child),
      ]),
    );
  }
  return value;
};

const sum = (object) =>
  Object.values(object || {}).reduce((total, value) => total + Number(value || 0), 0);

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const parseInvocation = (argv) => {
  const parsed = {};
  for (const arg of argv) {
    const match = /^--(mode|case|url)=(.*)$/.exec(arg);
    if (!match || Object.hasOwn(parsed, match[1]) || match[2] === "") {
      throw new Error(`illegal argument: ${arg}`);
    }
    parsed[match[1]] = match[2];
  }
  parsed.url ||= defaultUrl;
  const legal =
    (parsed.mode === "cold" && parsed.case === undefined) ||
    (parsed.mode === "hover" && parsed.case === undefined) ||
    (parsed.mode === "probe" && ["positive", "oracle-negative"].includes(parsed.case));
  if (!legal) throw new Error("legal tuples are cold, hover, probe/positive, probe/oracle-negative");
  return parsed;
};

const adapterAttestation = async (page) =>
  page.evaluate(async () => {
    if (!navigator.gpu) throw new Error("navigator.gpu unavailable");
    const adapter = await navigator.gpu.requestAdapter();
    if (!adapter) throw new Error("requestAdapter returned nil");
    const info = adapter.info || (adapter.requestAdapterInfo ? await adapter.requestAdapterInfo() : {});
    const description =
      info.description || [info.vendor, info.architecture, info.device].filter(Boolean).join(" ");
    return {
      isFallbackAdapter:
        typeof adapter.isFallbackAdapter === "boolean"
          ? adapter.isFallbackAdapter
          : typeof info.isFallbackAdapter === "boolean"
            ? info.isFallbackAdapter
            : null,
      description: description || "unreported",
      vendor: info.vendor || null,
      architecture: info.architecture || null,
      device: info.device || null,
      features: [...adapter.features].sort(),
    };
  });

const browserEnvironment = async (browser, page, effectiveUrl) => {
  const browserVersion = await browser.version();
  const pageEnvironment = await page.evaluate(() => ({
    width: innerWidth,
    height: innerHeight,
    dpr: devicePixelRatio,
    userAgent: navigator.userAgent,
    secureContext: isSecureContext,
  }));
  const cpu = os.cpus()[0]?.model || "unknown";
  const buildFile = path.join(repoRoot, "resources/public/js/main.js");
  return {
    hostname: os.hostname(),
    cpu,
    os: { platform: os.platform(), release: os.release(), architecture: os.arch() },
    browser: { version: browserVersion, ...pageEnvironment },
    viewport,
    app: {
      effectiveUrl,
      buildId: fs.existsSync(buildFile) ? sha256(fs.readFileSync(buildFile)) : null,
      openingHead: execFileSync("git", ["rev-parse", "HEAD"], {
        cwd: repoRoot,
        encoding: "utf8",
      }).trim(),
    },
  };
};

const readLive = (page) =>
  page.evaluate(() => ({
    counters: window.__ground.counters(),
    cache: window.__ground.layoutCache(),
    census: window.__ground.profileCensus(),
    ordinary: window.__ground.profileBlock(window.__shapingProfileIds.ordinary),
    largest: window.__ground.profileBlock(window.__shapingProfileIds.largest),
    report: window.__ground.report(),
  })).then(camelize);

const settle = async (page, navigationStartMs, consoleRows, timeoutMs = 120000) => {
  const deadline = Date.now() + timeoutMs;
  let lastSignature = null;
  let quietSince = null;
  let last = null;
  while (Date.now() < deadline) {
    last = await readLive(page);
    const signature = JSON.stringify(
      stable({
        counters: last.counters,
        cache: last.cache,
        census: last.census,
        ordinary: last.ordinary,
        largest: last.largest,
      }),
    );
    const populated =
      last.census.blocks > 0 &&
      last.census.liveTextSlots > 0 &&
      Boolean(last.largest) &&
      last.cache.size === last.cache.addressCount;
    if (populated && signature === lastSignature) {
      quietSince ||= Date.now();
      if (Date.now() - quietSince >= 2000) {
        const lastG8 = [...consoleRows].reverse().find((row) => row.text.startsWith("[SCENE-FACES/G8]"));
        return {
          timedOut: false,
          quietMs: Date.now() - quietSince,
          visualSettleMs: (lastG8?.atMs || Date.now()) - navigationStartMs,
          lastG8: lastG8 || null,
          live: last,
        };
      }
    } else {
      lastSignature = signature;
      quietSince = null;
    }
    await sleep(100);
  }
  return { timedOut: true, quietMs: 0, visualSettleMs: timeoutMs, live: last };
};

const corpusReceipt = (live, identity) => {
  const blockIds = identity?.blockIds || [];
  const missingBlockSlotIds = identity?.missingBlockSlotIds || [];
  const nonBlockSlotIds = identity?.nonBlockSlotIds || [];
  return {
    ...live.census,
    blockIdDigest: sha256(blockIds.join("\n")),
    blockSlotCount: identity?.blockSlotCount ?? null,
    missingBlockSlotIds,
    nonBlockSlotIds,
    liveSlotConservation:
      live.census.liveSlots ===
      (identity?.blockSlotCount ?? -1) + nonBlockSlotIds.length,
    pasteBlockPresent: Boolean(live.largest),
    pasteBlockTextLength: live.largest?.textLength || null,
    ordinaryOwnedAddresses: live.ordinary?.ownedAddresses ?? null,
    largestOwnedAddresses: live.largest?.ownedAddresses ?? null,
    pinnedBlocks: {
      ordinary: live.ordinary || null,
      largest: live.largest || null,
    },
  };
};

const productMinimumZoom = 0.1;

const storyboardGeometry = (ordinary, largest, reachability = null) => {
  const a = ordinary?.worldRect;
  const b = largest?.worldRect;
  if (!a || !b) return null;
  const verticalWorldGap = Math.max(
    0,
    Math.max(a.y, b.y) - Math.min(a.y + a.h, b.y + b.h),
  );
  const horizontalWorldGap = Math.max(
    0,
    Math.max(a.x, b.x) - Math.min(a.x + a.w, b.x + b.w),
  );
  return {
    productMinimumZoom,
    verticalWorldGap,
    horizontalWorldGap,
    minimumVerticalScreenGap: verticalWorldGap * productMinimumZoom,
    minimumHorizontalScreenGap: horizontalWorldGap * productMinimumZoom,
    viewport,
    // §9 AMENDED 2026-08-05 — RECORDED, never asserted.  `false` here is the
    // EXPECTED state on the real corpus and is precisely the measured fact
    // that forced the storyboard recut; the setup assertion below tests
    // reachability instead.
    coVisibleAtProductMinimumZoom:
      verticalWorldGap * productMinimumZoom < viewport.height &&
      horizontalWorldGap * productMinimumZoom < viewport.width,
    reachability,
  };
};

const environmentAssertions = (attestation, environment, corpus) => {
  const chromeMajor = Number((environment.browser.version.match(/(\d+)\./) || [])[1]);
  return {
    adapterAttested: typeof attestation.isFallbackAdapter === "boolean",
    nonFallbackAdapter: attestation.isFallbackAdapter === false,
    chromiumMajorAtLeast150: chromeMajor >= 150,
    viewport:
      environment.browser.width === viewport.width &&
      environment.browser.height === viewport.height &&
      environment.browser.dpr === viewport.deviceScaleFactor,
    corpus:
      corpus.blocks === expectedCorpus.blocks &&
      corpus.machineBlocks === expectedCorpus.machineBlocks &&
      corpus.servedChars === expectedCorpus.servedChars &&
      corpus.blockIdDigest === expectedCorpus.blockIdDigest &&
      corpus.blockSlotCount === expectedCorpus.blockSlots &&
      corpus.liveTextSlots === expectedCorpus.liveTextSlots &&
      corpus.missingBlockSlotIds.length === 0 &&
      corpus.nonBlockSlotIds.length === expectedCorpus.nonBlockSlots &&
      corpus.liveSlotConservation &&
      corpus.pasteBlockPresent &&
      corpus.pasteBlockTextLength === 24891,
  };
};

// ===========================================================================
// ELIGIBILITY SPLIT — §10 environment law AMENDED 2026-08-05 (Sid ruling,
// direct, no revalidation round).
//
// FUNCTIONAL eligibility = everything the COUNT assertions need: an attested
// adapter, the pinned Chromium major, the pinned viewport/DPR, and the pinned
// corpus.  Counts (packs, writes, geo lifecycles, cache misses/replacements,
// dirty marks) are properties of the product's own data flow — the rasterizer
// behind the WebGPU adapter cannot change them.
//
// TIMING eligibility = functional PLUS `nonFallbackAdapter`.  A fallback
// adapter (SwiftShader) is a CPU rasterizer and genuinely cannot prove the
// 52ms/12.0s bars — that law is UNCHANGED.
//
// Historical behavior allowed a fallback-adapter count-only diagnostic. The
// settlement does not invoke it: the paired matrix is retired, incomplete, and
// non-blocking. This code remains only so existing receipts stay intelligible;
// it is not evidence that all 20 rows ran or passed.
const functionalEligibilityFields = [
  "adapterAttested", "chromiumMajorAtLeast150", "viewport", "corpus",
];
const timingOnlyEligibilityFields = ["nonFallbackAdapter"];

const eligibilityOf = (checks) => {
  const fields = Object.keys(checks || {});
  // Fail-honest sum-check: an environment assertion that belongs to NEITHER
  // list is unclassified, and an unclassified environment field can never be
  // silently dropped from the functional set.
  const unclassifiedFields = fields.filter(
    (field) => !functionalEligibilityFields.includes(field) &&
      !timingOnlyEligibilityFields.includes(field),
  );
  const holds = (list) =>
    Boolean(checks) && unclassifiedFields.length === 0 &&
    list.every((field) => checks[field] === true);
  const functional = holds(functionalEligibilityFields);
  return {
    functional,
    timing: functional && holds(timingOnlyEligibilityFields),
    functionalFields: functionalEligibilityFields,
    timingOnlyFields: timingOnlyEligibilityFields,
    unclassifiedFields,
  };
};

const coldMeasurements = (settled) => {
  const { counters, cache, census } = settled.live;
  const fallback = counters.fallback || {};
  const assertions = {
    settled: !settled.timedOut,
    groundFallbackZero: fallback.ground === 0,
    fallbackSurfacesNamed:
      ["ground", "combinedTextOps", "settingsPanelText"].every((key) =>
        Object.hasOwn(fallback, key),
      ),
    cacheBound: cache.size === cache.addressCount,
    missConservation: cache.misses === sum(counters.layoutExecs),
    dirtyConservation:
      counters.dirty.entry - counters.dirty.exit === census.liveTextSlots,
    hoverColdZero: counters.dirty.hover === 0,
    providerFaultZero: counters.providerFault === 0,
  };
  return {
    assertions,
    wallBars: { visualSettleAtMost12000ms: settled.visualSettleMs <= 12000 },
    visualSettleMs: settled.visualSettleMs,
    quietMs: settled.quietMs,
    counters,
    cache,
    lastG8: settled.lastG8,
    report: settled.live.report,
  };
};

// ===========================================================================
// §9 storyboard — AMENDED 2026-08-05 (Sid ruling, direct, no revalidation
// round).  Executable-storyboard machinery + the exact-count derivation.
// ===========================================================================
//
// WHY THE RECUT.  target/shaping-correction/hover/receipt.json pins the two
// contracted blocks 8,142.94960061024 world units apart on Y (ordinary
// worldRect y=-21.656264568423964 h=70 → bottom 48.343735431576036; the
// 24,891-char paste block y=8191.293336041816).  The product zoom floor is
// 0.1 (`:space/zoom-min` default, ground.cljs:3580), so the SMALLEST screen
// gap the pair can ever have is 814.2949600610241px inside a 601px-tall
// viewport.  They are never co-visible, and at the boot camera BOTH are
// off-screen (`screenPoint` null in the banked receipt).  No lawful pointer
// script can perform seven DIRECT hover transitions between them.
//
// HARD LAW (unchanged by the amendment): no synthetic hover injection, no
// picking bypass, no direct camera-state poke.  Every reposition act is a
// literal input event on the product's own road —
//   page.mouse.move        → runtime/mouse.cljs :mousemove → ground/pointer-move!
//   page.mouse.down/up + moves → the :camera/pan verb (press on EMPTY ground)
//   page.mouse.wheel       → runtime/scroll.cljs → ground/handle-wheel!
//                            → the :camera/zoom-at-pointer verb
// The 52ms input→RAF bar exists to measure the REAL product pick path;
// faking hover would fake the receipt.
//
// ACT SEQUENCE (the ONE storyboard, recut to be performable):
//   A0  UNMEASURED reposition → paste block on screen, pointer parked empty
//   A1  UNMEASURED warm-up      empty→largest       (one RAF/store frame)
//   A2  UNMEASURED reposition → ordinary block on screen, pointer parked empty
//   A3  UNMEASURED positioning  empty→ordinary      (one RAF/store frame)
//   A4  UNMEASURED park         ordinary→empty, await frame quiet
//   A5  RESET counters (§8 hook) — only after the harness confirms hover is
//       nil on verified-empty ground and BOTH pinned layouts are cached by
//       A1/A3 (that caching is what makes measured layout-execs 0 lawful)
//   M1…M6  the six measured transitions, EACH preceded by its own UNMEASURED
//       reposition segment: empty→largest, empty→ordinary, empty→largest,
//       empty→ordinary, empty→largest, empty→ordinary
//   M7  the SEVENTH measured transition: ordinary→empty (the control) — no
//       reposition, lawful because M6 ends on ordinary and the verified
//       empty point is co-visible with the ordinary block itself
//
// EXACT COUNTS, RE-DERIVED FROM THIS ACT SEQUENCE (assert ==, never >=).
// ground.cljs pointer-move!'s idle branch does rebuild each non-nil side:
//     (when (not= uid @!hover)
//       (let [old @!hover] (reset! !hover uid)
//         (when old (rebuild-block! old :hover-paint))
//         (when uid (rebuild-block! uid :hover-paint))))
// The rebuild is consumed by anatomy: the hovered block gains a :ground-box
// attention rectangle. Block glyph tint and the slot text-paint token do not
// depend on hover. Therefore the text renderer correctly retains the cached
// GPU text data: no paint repack, write, dirty-text mark, or geo lifecycle.
// Each literal input still advances exactly one RAF and store-frame execution.
// The attention node's absence/presence is read from the live slot tree, so a
// changed hover atom alone cannot make this storyboard green.
//
//   act  transition       attention flips  RAF/store  G8  text/geo writes
//   M1   empty→largest           1             1       0         0
//   M2   empty→ordinary          1             1       0         0
//   M3   empty→largest           1             1       0         0
//   M4   empty→ordinary          1             1       0         0
//   M5   empty→largest           1             1       0         0
//   M6   empty→ordinary          1             1       0         0
//   M7   ordinary→empty          1             1       0         0
//   ---------------------------------------------------------------------
//   measured attention transitions       = 7
//   measured raf/store-frame executions  = 7 / 7
//   measured G8 text-pack lines           = 0
//   measured layout/paint/write/geo       = 0
//   measured cache misses                 = 0
//
// COUNTER WINDOWING.  Counters and the layout cache are sampled immediately
// BEFORE and immediately AFTER each measured act; every asserted total is the
// SUM of the seven per-act deltas.  Reposition segments are sampled the same
// way and their deltas are emitted as `unmeasuredSegments` telemetry — hover
// flips over blocks that slide under the parked pointer, `:dirty :camera`
// marks, and the frames a pan/zoom drives all land OUTSIDE every measured
// window and are RECORDED, never asserted.

const repositionInset = 30;
// 16 notches × deltaY 240 ⇒ factor 1.0015^-3840 ≈ 3.2e-3 from ANY legal zoom
// (≤ 8), so the product's own `(max zoom zoom-min)` clamp lands the camera on
// EXACTLY the 0.1 floor.  The wheel flow coalesces bursts by SUMMING dy
// (events.cljs >wheel m/relieve) and the zoom is multiplicative in dy, so the
// coalesced and uncoalesced paths reach the identical clamped zoom, and
// zoom-at-pointer holds the world point under the parked pointer fixed —
// the reached camera is a deterministic function of that point.
const zoomFloorNotches = 16;
const zoomFloorDeltaY = 240;
const panGestureBudget = 12;
// A pan gesture must exceed ground.cljs' 4.0px drag threshold, otherwise
// pointer-up! classifies the press as a TAP and the tap road sets a ground
// anchor / clears selections.  Below this the residual is accepted instead.
const minimumPanGesturePx = 16;

const readCounters = (page) =>
  page.evaluate(() => window.__ground.counters()).then(camelize);

const readCamera = (page) =>
  page.evaluate(() => window.__ground.camera()).then(camelize);

const readBlock = (page, id) =>
  page.evaluate((target) => window.__ground.profileBlock(target), id).then(camelize);

const readAttention = (page, id) =>
  page.evaluate((target) => window.__ground.profileAttention(target), id).then(camelize);

const readHover = (page) => page.evaluate(() => window.__ground.currentHover());

const readCacheCompact = async (page) =>
  compactCache(camelize(await page.evaluate(() => window.__ground.layoutCache())));

const g8LineCount = (consoleRows) =>
  consoleRows.filter((row) => row.text.startsWith("[SCENE-FACES/G8]")).length;

const addNumbers = (a, b) => {
  if (typeof a === "number" || typeof b === "number") {
    return Number(a || 0) + Number(b || 0);
  }
  if (a && typeof a === "object" && !Array.isArray(a)) {
    return Object.fromEntries(
      [...new Set([...Object.keys(a), ...Object.keys(b || {})])].map((key) => [
        key,
        addNumbers(a[key], b?.[key]),
      ]),
    );
  }
  return b ?? a;
};

// Frame quiet closes an UNMEASURED segment: pan/zoom arm a 400ms debounced
// camera settle (ground.cljs settle-debounce-ms), and a late frame from that
// settle must never bleed into the next measured window.
const awaitFrameQuiet = async (page, consoleRows, quietMs = 900, timeoutMs = 20000) => {
  const deadline = Date.now() + timeoutMs;
  let signature = null;
  let quietSince = null;
  while (Date.now() < deadline) {
    const next = JSON.stringify(
      stable({ counters: await readCounters(page), g8: g8LineCount(consoleRows) }),
    );
    if (next === signature) {
      quietSince ||= Date.now();
      if (Date.now() - quietSince >= quietMs) return { timedOut: false };
    } else {
      signature = next;
      quietSince = null;
    }
    await sleep(60);
  }
  return { timedOut: true };
};

const emptyPointCandidates = () => {
  const xs = [];
  for (let x = repositionInset; x <= viewport.width - repositionInset; x += 88) xs.push(x);
  const ys = [];
  for (let y = repositionInset; y <= viewport.height - repositionInset; y += 67) ys.push(y);
  return xs.flatMap((x) => ys.map((y) => ({ x, y })));
};

// Emptiness is decided by the PRODUCT's own pick: pointer-move!'s idle branch
// and pointer-down!'s press record filter the identical `pick-at` address, so
// `currentHover() === null` proves a press at this point resolves against the
// space claim (:camera/pan) and can never grab a block (:block/move).
const findEmptyPoint = async (page, preferred) => {
  const distance = (point) =>
    (point.x - preferred.x) ** 2 + (point.y - preferred.y) ** 2;
  const ranked = emptyPointCandidates().sort((p, q) => distance(p) - distance(q));
  for (const point of ranked) {
    await page.mouse.move(point.x, point.y);
    if ((await readHover(page)) === null) return point;
  }
  return null;
};

const zoomToProductFloor = async (page, point, consoleRows) => {
  await page.mouse.move(point.x, point.y);
  for (let index = 0; index < zoomFloorNotches; index += 1) {
    await page.mouse.wheel({ deltaY: zoomFloorDeltaY });
    await sleep(20);
  }
  await awaitFrameQuiet(page, consoleRows);
  return readCamera(page);
};

// The :camera/pan verb sets camera from the pointer's SCREEN delta 1:1
// (ground.cljs :camera/pan :move), so the camera delta a gesture produces IS
// its pointer delta.  Chunked because the pointer cannot leave the viewport.
const panCameraBy = async (page, delta, consoleRows) => {
  const remaining = { x: delta.x, y: delta.y };
  const gestures = [];
  for (let index = 0; index < panGestureBudget; index += 1) {
    if (Math.abs(remaining.x) < 1 && Math.abs(remaining.y) < 1) break;
    const start = await findEmptyPoint(page, {
      x: remaining.x >= 0 ? repositionInset : viewport.width - repositionInset,
      y: remaining.y >= 0 ? repositionInset : viewport.height - repositionInset,
    });
    if (!start) return { gestures, remaining, emptyGroundFound: false };
    const end = {
      x: Math.min(viewport.width - repositionInset,
        Math.max(repositionInset, start.x + remaining.x)),
      y: Math.min(viewport.height - repositionInset,
        Math.max(repositionInset, start.y + remaining.y)),
    };
    const step = { x: end.x - start.x, y: end.y - start.y };
    if (Math.hypot(step.x, step.y) < minimumPanGesturePx) break;
    const cameraBefore = await readCamera(page);
    await page.mouse.down();
    // The first move crosses the product drag threshold and CLAIMS the verb;
    // the second is the measured-verb-free pan continuation (same gesture
    // shape as G5's camera-pan row).
    await page.mouse.move(start.x + step.x / 2, start.y + step.y / 2);
    await page.mouse.move(end.x, end.y);
    await page.mouse.up();
    await awaitFrameQuiet(page, consoleRows);
    const cameraAfter = await readCamera(page);
    gestures.push({ start, end, cameraBefore, cameraAfter });
    remaining.x -= cameraAfter.x - cameraBefore.x;
    remaining.y -= cameraAfter.y - cameraBefore.y;
  }
  return { gestures, remaining, emptyGroundFound: true };
};

// ONE unmeasured reposition segment: park on empty ground, zoom to the
// product floor, pan until the target is on screen, PROVE a hoverable point
// on it through the real pick path, then park on empty again and go quiet.
const repositionTo = async (page, targetId, consoleRows) => {
  const countersBefore = await readCounters(page);
  const cacheBefore = await readCacheCompact(page);
  const g8Before = g8LineCount(consoleRows);
  const attempts = [];
  let targetPoint = null;
  for (let attempt = 0; attempt < 3 && !targetPoint; attempt += 1) {
    const park = await findEmptyPoint(page, {
      x: viewport.width / 2,
      y: repositionInset,
    });
    if (!park) break;
    const camera = attempt === 0
      ? await zoomToProductFloor(page, park, consoleRows)
      : await readCamera(page);
    const block = await readBlock(page, targetId);
    if (!block?.worldRect) break;
    const centre = {
      x: block.worldRect.x + block.worldRect.w / 2,
      y: block.worldRect.y + block.worldRect.h / 2,
    };
    // screen = world·zoom + camera  (the inverse of ground.cljs screen->world)
    const wanted = {
      x: viewport.width / 2 - centre.x * camera.zoom - camera.x,
      y: viewport.height / 2 - centre.y * camera.zoom - camera.y,
    };
    const pan = await panCameraBy(page, wanted, consoleRows);
    const placed = await readBlock(page, targetId);
    const candidate = placed?.screenPoint || null;
    let picked = null;
    if (candidate) {
      await page.mouse.move(candidate.x, candidate.y);
      picked = await readHover(page);
      if (picked === targetId) targetPoint = candidate;
    }
    attempts.push({ attempt, camera, wanted, pan, candidate, picked });
  }
  const emptyPoint = await findEmptyPoint(page, {
    x: viewport.width / 2,
    y: repositionInset,
  });
  await awaitFrameQuiet(page, consoleRows);
  const countersAfter = await readCounters(page);
  const cacheAfter = await readCacheCompact(page);
  return {
    target: targetId,
    targetPoint,
    emptyPoint,
    hoverAtClose: await readHover(page),
    camera: await readCamera(page),
    attempts,
    // UNMEASURED-SEGMENT TELEMETRY — reported, never asserted (§9 amended).
    telemetry: {
      counters: diffNumbers(countersAfter, countersBefore),
      cache: diffNumbers(cacheAfter, cacheBefore),
      g8Lines: g8LineCount(consoleRows) - g8Before,
    },
  };
};

// `from`/`target` are the act's hover state at window open and at window
// close. Hover is consumed by the anatomy attention rectangle. It does not
// change the block glyph tint or text payload, so it must not repack text.
const storyboardActPlan = [
  { act: "M1", from: null, target: largestId, transition: "empty→largest", reposition: true },
  { act: "M2", from: null, target: ordinaryId, transition: "empty→ordinary", reposition: true },
  { act: "M3", from: null, target: largestId, transition: "empty→largest", reposition: true },
  { act: "M4", from: null, target: ordinaryId, transition: "empty→ordinary", reposition: true },
  { act: "M5", from: null, target: largestId, transition: "empty→largest", reposition: true },
  { act: "M6", from: null, target: ordinaryId, transition: "empty→ordinary", reposition: true },
  { act: "M7", from: ordinaryId, target: null, transition: "ordinary→empty", reposition: false },
];

// The derivation above, made machine-readable so the numbers travel with the
// run instead of living only in this file's comments.
const storyboardDerivation = {
  amendedOn: "2026-08-05",
  ruling: "Sid, direct — fix now, no contract-revalidation round",
  forcingGeometry: {
    source: "target/shaping-correction/hover/receipt.json",
    verticalWorldGap: 8142.94960061024,
    productMinimumZoom,
    minimumVerticalScreenGap: 814.2949600610241,
    viewportHeight: viewport.height,
    coVisibleEverAtAnyLegalZoom: false,
    bothOffScreenAtBootCamera: true,
  },
  consumptionLaw:
    "ground.cljs pointer-move! rebuilds each non-nil side so anatomy can add " +
    "or remove its :ground-box attention rectangle. The block glyph tint and " +
    "slot paint token do not depend on hover, so runtime/render must retain " +
    "the text GPU allocation: zero text repacks, writes, and geo lifecycle " +
    "changes. The attention node itself is read back for every act.",
  acts: storyboardActPlan.map((step) => ({
    act: step.act,
    transition: step.transition,
    hoverAtOpen: step.from,
    hoverAtClose: step.target,
    nonNilHoverSides: [step.from, step.target].filter(Boolean).length,
    attentionTransition: true,
    hoverPaintRepacks: 0,
    dirtyHover: 0,
    slotTextWrites: 0,
    geoInPlace: 0,
    rafFrames: 1,
    storeFrameExecs: 1,
    g8Lines: 0,
    layoutExecs: 0,
    cacheMisses: 0,
  })),
  totals: {
    measuredActs: 7,
    hoverPaintRepacks: 0,
    paintRepacksAllCauses: 0,
    dirtyHover: 0,
    dirtyAllCauses: 0,
    slotTextWrites: 0,
    geoInPlace: 0,
    geoAllLifecycles: 0,
    rafFrames: 7,
    storeFrameExecs: 7,
    g8Lines: 0,
    layoutExecsAllCauses: 0,
    cacheMisses: 0,
    cacheHitsExpectedRunRecorded: 7,
  },
  supersedes: {
    hoverPaintRepacks: 13,
    derivation: "six ordinary↔largest × 2 slots + the control's 1",
    invalidatedBecause:
      "two slots per act requires the pinned pair to be co-visible; the " +
      "forcing geometry above makes that impossible at every legal zoom.",
  },
};

const moveAndAwaitFrame = async (page, point, expectedHover, consoleRows) => {
  const hoverAtOpen = await page.evaluate(() => window.__ground.currentHover());
  const sourceAttentionBefore = hoverAtOpen ? await readAttention(page, hoverAtOpen) : null;
  const targetAttentionBefore = expectedHover ? await readAttention(page, expectedHover) : null;
  const before = camelize(await page.evaluate(() => window.__ground.counters()));
  const started = Date.now();
  const g8Before = consoleRows.filter((row) => row.text.startsWith("[SCENE-FACES/G8]")).length;
  await page.mouse.move(point.x, point.y);
  const deadline = Date.now() + 10000;
  let after;
  while (Date.now() < deadline) {
    after = camelize(await page.evaluate(() => window.__ground.counters()));
    const hover = await page.evaluate(() => window.__ground.currentHover());
    if (after.rafFrames >= before.rafFrames + 1 && hover === expectedHover) break;
    await sleep(10);
  }
  const completedAt = Date.now();
  // Let an illicit second product frame surface before closing the delta.
  await sleep(100);
  after = camelize(await page.evaluate(() => window.__ground.counters()));
  const g8Rows = consoleRows
    .filter((row) => row.text.startsWith("[SCENE-FACES/G8]"))
    .slice(g8Before);
  const sourceAttentionAfter = hoverAtOpen ? await readAttention(page, hoverAtOpen) : null;
  const targetAttentionAfter = expectedHover ? await readAttention(page, expectedHover) : null;
  return {
    // Hover intentionally emits no G8 text-pack line. The product-observable
    // completion edge is the RAF/store-frame counter paired with the hover
    // identity change, before the duplicate-frame watch.
    inputToRafMs: completedAt - started,
    inputToCounterMs: completedAt - started,
    rafDelta: after.rafFrames - before.rafFrames,
    storeFrameDelta: after.storeFrameExecs - before.storeFrameExecs,
    g8Delta: g8Rows.length,
    hoverAfter: await page.evaluate(() => window.__ground.currentHover()),
    sourceAttentionBefore,
    sourceAttentionAfter,
    targetAttentionBefore,
    targetAttentionAfter,
    // §9 AMENDED 2026-08-05 — per-act counter windowing.  `before` is read
    // immediately before the input and `after` immediately after the act
    // closes, so every asserted storyboard total is the SUM of these deltas
    // and no reposition-segment activity can enter a measured window.
    hoverBefore: hoverAtOpen,
    countersBefore: before,
    countersAfter: after,
    deltas: diffNumbers(after, before),
  };
};

const hoverMeasurements = async (page, settled, consoleRows) => {
  const baseSetup = {
    ordinaryPresent: Boolean(settled.live.ordinary),
    largestPresent: Boolean(settled.live.largest),
    ordinaryOwnedAddressCountOne: settled.live.ordinary?.ownedAddresses === 1,
    largestOwnedAddressCountOne: settled.live.largest?.ownedAddresses === 1,
  };
  const unmeasuredSegments = [];
  const notReady = (setupAssertions, geometry, extra = {}) => ({
    setupAssertions,
    storyboardGeometry: geometry,
    storyboardDerivation,
    unmeasuredSegments,
    assertions: { storyboardReady: false },
    wallBars: {},
    transitions: [],
    ...extra,
  });
  if (!Object.values(baseSetup).every(Boolean)) {
    return notReady(
      {
        ...baseSetup,
        largestReachable: false,
        ordinaryReachable: false,
        emptyGroundAvailable: false,
        warmupPairOne: false,
        positioningPairOne: false,
        positioningEndsOnOrdinary: false,
      },
      storyboardGeometry(settled.live.ordinary, settled.live.largest),
    );
  }

  // ---- A0 · UNMEASURED reposition: bring the paste block on screen --------
  const a0 = await repositionTo(page, largestId, consoleRows);
  unmeasuredSegments.push({ act: "A0-reposition-largest", ...a0 });

  // ---- A1 · UNMEASURED warm-up hover: empty → largest ---------------------
  const warmup = a0.targetPoint
    ? await moveAndAwaitFrame(page, a0.targetPoint, largestId, consoleRows)
    : null;

  // ---- A2 · UNMEASURED reposition: bring the ordinary block on screen -----
  const a2 = await repositionTo(page, ordinaryId, consoleRows);
  unmeasuredSegments.push({ act: "A2-reposition-ordinary", ...a2 });

  // ---- A3 · UNMEASURED positioning hover: empty → ordinary ----------------
  const positioning = a2.targetPoint
    ? await moveAndAwaitFrame(page, a2.targetPoint, ordinaryId, consoleRows)
    : null;

  const reachability = {
    largest: {
      onScreen: Boolean(a0.targetPoint),
      hoverPointProven: Boolean(a0.targetPoint),
      point: a0.targetPoint,
      emptyPoint: a0.emptyPoint,
      camera: a0.camera,
    },
    ordinary: {
      onScreen: Boolean(a2.targetPoint),
      hoverPointProven: Boolean(a2.targetPoint),
      point: a2.targetPoint,
      emptyPoint: a2.emptyPoint,
      camera: a2.camera,
    },
  };
  const geometry = storyboardGeometry(
    settled.live.ordinary, settled.live.largest, reachability,
  );
  // §9 AMENDED 2026-08-05 — the setup assertion checks what the EXECUTABLE
  // storyboard actually needs: each pinned target reachable by literal camera
  // input and PROVEN hoverable through the product pick path at the reached
  // legal zoom, plus empty ground to park on and to control against.
  // Co-visibility is recorded in `storyboardGeometry`, never required.
  const setupAssertions = {
    ...baseSetup,
    largestReachable: reachability.largest.hoverPointProven,
    ordinaryReachable: reachability.ordinary.hoverPointProven,
    emptyGroundAvailable: Boolean(a0.emptyPoint) && Boolean(a2.emptyPoint),
    warmupPairOne:
      warmup?.rafDelta === 1 && warmup?.storeFrameDelta === 1 && warmup?.g8Delta === 0,
    positioningPairOne:
      positioning?.rafDelta === 1 && positioning?.storeFrameDelta === 1 &&
      positioning?.g8Delta === 0,
    positioningEndsOnOrdinary: positioning?.hoverAfter === ordinaryId,
  };
  if (!Object.values(setupAssertions).every(Boolean)) {
    return notReady(setupAssertions, geometry, {
      preReset: { warmup, positioning },
    });
  }

  // ---- A4 · UNMEASURED park on empty ground, then quiet -------------------
  const parkPoint = await findEmptyPoint(page, a2.emptyPoint);
  await awaitFrameQuiet(page, consoleRows);
  const hoverAtReset = await readHover(page);
  // ---- A5 · RESET counters (§8 hook) --------------------------------------
  await page.evaluate(() => window.__ground.resetCounters());

  // ---- M1…M7 · the seven MEASURED transitions ----------------------------
  const measured = [];
  let controlEmptyPoint = parkPoint;
  let planAborted = null;
  for (const step of storyboardActPlan) {
    let point = controlEmptyPoint;
    if (step.reposition) {
      const segment = await repositionTo(page, step.target, consoleRows);
      unmeasuredSegments.push({ act: `${step.act}-reposition`, ...segment });
      if (!segment.targetPoint || !segment.emptyPoint) {
        planAborted = step.act;
        break;
      }
      controlEmptyPoint = segment.emptyPoint;
      point = segment.targetPoint;
    }
    if (!point) {
      planAborted = step.act;
      break;
    }
    const cacheBefore = await readCacheCompact(page);
    const row = await moveAndAwaitFrame(page, point, step.target, consoleRows);
    const cacheAfter = await readCacheCompact(page);
    measured.push({
      act: step.act,
      transition: step.transition,
      expectedHover: step.target,
      point,
      cacheBefore,
      cacheAfter,
      cacheDeltas: diffNumbers(cacheAfter, cacheBefore),
      ...row,
    });
  }

  const counters = camelize(await page.evaluate(() => window.__ground.counters()));
  const cache = camelize(await page.evaluate(() => window.__ground.layoutCache()));
  // Every asserted total is the SUM of the seven measured per-act windows —
  // NOT the post-reset absolute counters, which also carry the reposition
  // segments that sit between the acts.
  const measuredTotals = measured.map((row) => row.deltas).reduce(addNumbers, {});
  const measuredCacheTotals =
    measured.map((row) => row.cacheDeltas).reduce(addNumbers, {});
  const derivedTotals = storyboardDerivation.totals;
  const assertions = {
    storyboardReady: true,
    storyboardPlanComplete: planAborted === null,
    resetOnEmptyGround: hoverAtReset === null,
    measuredTransitionsSeven: measured.length === derivedTotals.measuredActs,
    hoverSequenceExact: measured.every(
      (row, index) => row.hoverAfter === storyboardActPlan[index].target,
    ),
    // the derivation's precondition: every measured act opens from the
    // planned hover state, so exactly ONE non-nil side is crossed per act
    hoverEntrySequenceExact: measured.every(
      (row, index) => row.hoverBefore === storyboardActPlan[index].from,
    ),
    // one product frame and no text-pack G8 line per measured attention act
    exactlyOneRafEach: measured.every((row) =>
      row.rafDelta === 1 && row.storeFrameDelta === 1 && row.g8Delta === 0),
    attentionTransitionVisible: measured.every((row, index) => {
      const step = storyboardActPlan[index];
      const removed = step.from === null ||
        (row.sourceAttentionBefore?.present === true &&
         row.sourceAttentionAfter?.present === false);
      const added = step.target === null ||
        (row.targetAttentionBefore?.present === false &&
         row.targetAttentionAfter?.present === true);
      return removed && added;
    }),
    // Optional chaining ONLY guards an aborted plan (empty totals object) —
    // an absent field yields `undefined`, which fails every equality below,
    // so a broken run can never read as green.
    measuredRafFramesSeven: measuredTotals.rafFrames === derivedTotals.rafFrames,
    measuredStoreFramesSeven:
      measuredTotals.storeFrameExecs === derivedTotals.storeFrameExecs,
    layoutExecsZero:
      sum(measuredTotals.layoutExecs) === derivedTotals.layoutExecsAllCauses,
    cacheMissesZero: measuredCacheTotals.misses === derivedTotals.cacheMisses,
    hoverPaintRepacksZero:
      measuredTotals.paintRepacks?.hoverPaint === derivedTotals.hoverPaintRepacks,
    paintRepacksAllZero:
      sum(measuredTotals.paintRepacks) === derivedTotals.paintRepacksAllCauses,
    dirtyHoverZero: measuredTotals.dirty?.hover === derivedTotals.dirtyHover,
    dirtyAllZero:
      sum(measuredTotals.dirty) === derivedTotals.dirtyAllCauses,
    slotTextWritesZero:
      measuredTotals.slotTextWrites === derivedTotals.slotTextWrites,
    geoInPlaceZero: measuredTotals.geo?.inPlace === derivedTotals.geoInPlace,
    geoAllZero:
      sum(measuredTotals.geo) === derivedTotals.geoAllLifecycles,
    cacheBound: cache.size === cache.addressCount,
  };
  return {
    setupAssertions,
    storyboardGeometry: geometry,
    storyboardDerivation,
    preReset: { warmup, positioning, parkPoint, hoverAtReset },
    planAborted,
    assertions,
    wallBars: {
      everyTransitionAtMost52ms: measured.every((row) =>
        typeof row.inputToRafMs === "number" && row.inputToRafMs <= 52),
    },
    transitions: measured,
    measuredTotals,
    measuredCacheTotals,
    // UNMEASURED-SEGMENT TELEMETRY — reported, never asserted (§9 amended).
    unmeasuredSegments,
    counters,
    cache,
  };
};

const diffNumbers = (after, before) => {
  if (typeof after === "number" || typeof before === "number") {
    return Number(after || 0) - Number(before || 0);
  }
  if (after && typeof after === "object" && !Array.isArray(after)) {
    return Object.fromEntries(
      [...new Set([...Object.keys(before || {}), ...Object.keys(after)])].map((key) => [
        key,
        diffNumbers(after[key], before?.[key]),
      ]),
    );
  }
  return after;
};

const numericLeaves = (value) => {
  if (typeof value === "number") return [value];
  if (!value || typeof value !== "object") return [];
  return Object.values(value).flatMap(numericLeaves);
};

const zeroTree = (value) => numericLeaves(value).every((number) => number === 0);

const compactCache = (cache) => ({
  size: cache.size,
  addressCount: cache.addressCount,
  hits: cache.hits,
  misses: cache.misses,
  replacements: cache.replacements,
  idDigest: cache.idDigest,
  oracleChecks: cache.oracleChecks,
  oracleMismatches: cache.oracleMismatches,
});

const compactCensus = (census) => ({
  blocks: census.blocks,
  machineBlocks: census.machineBlocks,
  servedChars: census.servedChars,
  liveSlots: census.liveSlots,
  liveTextSlots: census.liveTextSlots,
  liveAddresses: census.liveAddresses,
  hover: census.hover,
  camera: census.camera,
});

const profileState = async (page) => {
  const state = await readLive(page);
  const targets = camelize(await page.evaluate(() => window.__ground.profileTargets()));
  return {
    ...state,
    cache: compactCache(state.cache),
    census: compactCensus(state.census),
    targets,
  };
};

const waitForProfileQuiet = async (page, timeoutMs = 30000) => {
  const deadline = Date.now() + timeoutMs;
  let signature = null;
  let quietSince = null;
  let last = null;
  while (Date.now() < deadline) {
    last = await profileState(page);
    const next = JSON.stringify(stable({
      counters: last.counters,
      cache: last.cache,
      census: last.census,
      targets: last.targets,
    }));
    if (next === signature) {
      quietSince ||= Date.now();
      if (Date.now() - quietSince >= 2000) return { timedOut: false, state: last };
    } else {
      signature = next;
      quietSince = null;
    }
    await sleep(100);
  }
  return { timedOut: true, state: last };
};

const openTrialPage = async (browser, invocation, consoleRows) => {
  const createContext = browser.createBrowserContext || browser.createIncognitoBrowserContext;
  const context = await createContext.call(browser);
  const page = await context.newPage();
  await page.setViewport(viewport);
  page.on("console", (message) =>
    consoleRows.push({ type: message.type(), text: message.text(), atMs: Date.now() }));
  page.on("pageerror", (error) =>
    consoleRows.push({ type: "pageerror", text: String(error), atMs: Date.now() }));
  const started = Date.now();
  await page.goto(invocation.url, { waitUntil: "domcontentloaded", timeout: 30000 });
  await page.evaluate(
    (ids) => { window.__shapingProfileIds = ids; },
    { ordinary: ordinaryId, largest: largestId },
  );
  await page.waitForFunction(() => window.__ground?.profileTargets, { timeout: 30000 });
  const settled = await settle(page, started, consoleRows);
  return { context, page, settled };
};

const trialSnapshot = (state, rowState) => ({
  cache: {
    size: state.cache.size,
    addressCount: state.cache.addressCount,
    idDigest: state.cache.idDigest,
  },
  census: state.census,
  targets: state.targets,
  rowState,
});

const semanticTrialSnapshot = (state, rowState) => {
  const snapshot = trialSnapshot(state, rowState);
  const camera = snapshot.census?.camera;
  if (!camera) return snapshot;
  return {
    ...snapshot,
    census: {
      ...snapshot.census,
      // Product camera math is f64; a wheel gesture followed by its literal
      // inverse can leave representation-only 1e-13 residue. The raw values
      // remain in the trial receipt, while restore identity uses sub-pixel
      // semantic precision rather than pretending those bits are visible
      // state.
      camera: Object.fromEntries(Object.entries(camera).map(([key, value]) => [
        key, Math.round(value * 1e9) / 1e9,
      ])),
    },
  };
};

const pairedTrialSnapshot = (state, rowState) => {
  const snapshot = trialSnapshot(state, rowState);
  // Each trial opens an independent live page. Camera is durable UI state and
  // may settle between those page openings, even though it is unrelated to a
  // non-camera row's X state. Keep the raw values in every receipt and keep
  // camera in the within-trial restore check; exclude only that unrelated
  // founding value from the two-page paired-prestate identity comparison.
  return {
    ...snapshot,
    census: {
      ...snapshot.census,
      camera: "recorded-outside-paired-prestate",
    },
    rowState:
      snapshot.rowState && Object.hasOwn(snapshot.rowState, "camera")
        ? { ...snapshot.rowState, camera: "recorded-outside-paired-prestate" }
        : snapshot.rowState,
  };
};

const runPairedRow = async (browser, invocation, consoleRows, row) => {
  const trials = [];
  const prepared = [];
  try {
    // Bank BOTH isolated pre-states before either measured action. Camera
    // restores traverse the durable UI road and may settle to the server; a
    // sequential open-after-restore scheme would compare trial 1's founding
    // state to trial 2's post-trial-1 durable state instead of comparing the
    // two isolated trials promised by the contract.
    for (let trialIndex = 0; trialIndex < 2; trialIndex += 1) {
      const opened = await openTrialPage(browser, invocation, consoleRows);
      const { page, settled } = opened;
      try {
        // `consoleRows` reaches the row because §9's literal reposition road
        // (rows b/l) closes its unmeasured segments on frame quiet, which is
        // read from the product's own G8 console line.
        const rowState = await row.setup(page, settled, consoleRows);
        const preQuiet = await waitForProfileQuiet(page);
        const preState = preQuiet.state;
        const preHash = sha256(JSON.stringify(stable(
          pairedTrialSnapshot(preState, rowState.receipt),
        )));
        prepared.push({ ...opened, trialIndex, rowState, preQuiet, preState, preHash });
      } catch (error) {
        await opened.context.close();
        throw error;
      }
    }

    for (const preparedTrial of prepared) {
      const { page, trialIndex, rowState, preQuiet, preState, preHash } = preparedTrial;
      await page.evaluate(() => window.__ground.resetCounters());
      const before = await profileState(page);
      await row.action(page, rowState);
      const afterQuiet = await waitForProfileQuiet(page);
      const after = afterQuiet.state;
      const deltas = {
        counters: diffNumbers(after.counters, before.counters),
        cache: diffNumbers(after.cache, before.cache),
        census: diffNumbers(after.census, before.census),
      };
      const rowAssertions = row.assert({ before, after, deltas, rowState, afterQuiet });
      await row.restore(page, rowState);
      const restoreQuiet = await waitForProfileQuiet(page);
      const restoredHash = sha256(JSON.stringify(stable(
        semanticTrialSnapshot(restoreQuiet.state, rowState.receipt),
      )));
      const semanticPreHash = sha256(JSON.stringify(stable(
        semanticTrialSnapshot(preState, rowState.receipt),
      )));
      const assertions = {
        preQuiet: !preQuiet.timedOut,
        actionQuiet: !afterQuiet.timedOut,
        restoreQuiet: !restoreQuiet.timedOut,
        preBound:
          before.cache.size === before.cache.addressCount &&
          before.cache.addressCount === before.census.liveAddresses,
        postBound:
          after.cache.size === after.cache.addressCount &&
          after.cache.addressCount === after.census.liveAddresses,
        storeFramePerRaf: deltas.counters.storeFrameExecs <= deltas.counters.rafFrames,
        restoredSemanticState: restoredHash === semanticPreHash,
        ...rowAssertions,
      };
      trials.push({
        trial: trialIndex + 1,
        preStateSha256: preHash,
        assertions,
        deltas,
        before: { cache: before.cache, census: before.census },
        after: { cache: after.cache, census: after.census },
        restoredStateSha256: restoredHash,
        receipt: rowState.receipt,
      });
    }
  } finally {
    await Promise.all(prepared.map(({ context }) => context.close()));
  }
  const preStatesByteEqual =
    trials[0].preStateSha256 === trials[1].preStateSha256;
  const trialDeltasEqual =
    JSON.stringify(stable(trials[0].deltas)) === JSON.stringify(stable(trials[1].deltas));
  return {
    name: row.name,
    trials,
    assertions: {
      preStatesByteEqual,
      trialDeltasEqual,
      trialsGreen: trials.every((trial) => Object.values(trial.assertions).every(Boolean)),
    },
  };
};

const cacheConservation = ({ deltas }) =>
  deltas.cache.misses === sum(deltas.counters.layoutExecs) &&
  deltas.counters.oracleLayoutExecs === deltas.cache.oracleChecks;

const boundAfter = ({ after }) => after.cache.size === after.cache.addressCount;

const oneGeoWrite = (geo) =>
  geo.fresh + geo.inPlace + geo.recloned + geo.capacityGrown === 1;

const numericPaths = (value, prefix = []) => {
  if (typeof value === "number") return [[prefix.join("."), value]];
  if (!value || typeof value !== "object" || Array.isArray(value)) return [];
  return Object.entries(value).flatMap(([key, child]) =>
    numericPaths(child, [...prefix, key]));
};

const unnamedZero = ({ deltas }, named = []) => {
  const governed = {
    counters: {
      layoutExecs: deltas.counters.layoutExecs,
      oracleLayoutExecs: deltas.counters.oracleLayoutExecs,
      paintRepacks: deltas.counters.paintRepacks,
      geo: deltas.counters.geo,
      slotTextWrites: deltas.counters.slotTextWrites,
      dirty: deltas.counters.dirty,
      providerFault: deltas.counters.providerFault,
      fallback: deltas.counters.fallback,
    },
    cache: {
      misses: deltas.cache.misses,
      replacements: deltas.cache.replacements,
      size: deltas.cache.size,
      addressCount: deltas.cache.addressCount,
      oracleChecks: deltas.cache.oracleChecks,
      oracleMismatches: deltas.cache.oracleMismatches,
    },
  };
  const allowed = new Set(named);
  return numericPaths(governed).every(([path, value]) =>
    allowed.has(path) || value === 0);
};

// ===========================================================================
// ROW (b)/(l) PAIR SELECTION — §10 G4 row (b) AMENDED 2026-08-05 (Sid ruling,
// direct, no revalidation round).
//
// WHY THE RECUT.  Row (b)'s literal wording is "hover flip pinned
// largest→ordinary … +2".  The +2 requires BOTH sides of the flip to be
// non-nil in ONE pointer move, i.e. the two pinned blocks must be on screen
// at the same time.  The banked corpus geometry
// (target/shaping-correction/hover/receipt.json) puts them 8,142.94960061024
// world units apart on Y; the product zoom floor is 0.1
// (`:space/zoom-min`, ground.cljs:3580), so the SMALLEST screen gap the pair
// can ever have is 814.2949600610241px in a 601px-tall viewport.  They are
// never co-visible at any legal zoom — the identical defect §9 was amended
// for, one row later.
//
// THE RECUT. The flip runs between two ADJACENT
// CO-VISIBLE blocks discovered from the live corpus at run time:
//   · adjacency  = consecutive in the ground's own vertical world order
//                  (sorted by worldRect y, then x, then id), so no third
//                  block's origin lies between them;
//   · co-visibility = BOTH blocks project a screen point under the CURRENT
//                  camera (ground.cljs profile-block's own `visible?`), and
//                  each point PROVES its pick through the product's own hover
//                  road before the pair is accepted;
//   · shape      = each block owns exactly ONE live slot and ONE layout
//                  address. The attention box hands off while the text paint
//                  token remains invariant; the two cache hits are diagnostic,
//                  and text repacks/writes/geometry stay zero.
// The pair is never hardcoded: its identity, world rects, screen points, and
// every rejected candidate are recorded in the row receipt.
//
// HARD LAW (unchanged): no synthetic hover/picking injection and no direct
// camera poke.  The corpus is reached through §9's literal reposition road
// (`repositionTo` — wheel to the product zoom floor + empty-ground pointer
// drags) and every hover comes from `page.mouse.move`.
const singleSlotSingleAddress = (block) =>
  block?.slotCount === 1 && block?.ownedAddresses === 1;

const findAdjacentCoVisiblePair = async (page) => {
  const blocks = camelize(await page.evaluate(() => window.__ground.profileBlocks()));
  const ordered = Object.values(blocks)
    .filter((block) => block?.worldRect)
    .sort(
      (a, b) =>
        a.worldRect.y - b.worldRect.y ||
        a.worldRect.x - b.worldRect.x ||
        (a.id < b.id ? -1 : a.id > b.id ? 1 : 0),
    );
  const rejected = [];
  for (let index = 0; index + 1 < ordered.length; index += 1) {
    const from = ordered[index];
    const to = ordered[index + 1];
    if (!from.screenPoint || !to.screenPoint) continue;
    if (!singleSlotSingleAddress(from) || !singleSlotSingleAddress(to)) continue;
    await page.mouse.move(from.screenPoint.x, from.screenPoint.y);
    const fromHover = await readHover(page);
    await page.mouse.move(to.screenPoint.x, to.screenPoint.y);
    const toHover = await readHover(page);
    if (fromHover === from.id && toHover === to.id) {
      // Park the flip's X side, OUTSIDE the measured window, so the measured
      // act is exactly one `X→Y` pointer move (the §10 arrow law).
      await page.mouse.move(from.screenPoint.x, from.screenPoint.y);
      if ((await readHover(page)) === from.id) {
        return { from, to, orderIndex: index, orderedBlocks: ordered.length, rejected };
      }
    }
    rejected.push({ from: from.id, to: to.id, fromHover, toHover });
    if (rejected.length >= 8) break;
  }
  return { from: null, to: null, orderIndex: null, orderedBlocks: ordered.length, rejected };
};

const hoverPairReceipt = (pair, recut) => ({
  recut,
  from: pair.from?.id || null,
  to: pair.to?.id || null,
  fromWorldRect: pair.from?.worldRect || null,
  toWorldRect: pair.to?.worldRect || null,
  fromScreenPoint: pair.from?.screenPoint || null,
  toScreenPoint: pair.to?.screenPoint || null,
  fromOwnedAddresses: pair.from?.ownedAddresses ?? null,
  toOwnedAddresses: pair.to?.ownedAddresses ?? null,
  verticalWorldOrderIndex: pair.orderIndex,
  orderedBlocks: pair.orderedBlocks,
  rejectedCandidatePairs: pair.rejected,
});

// The flip's shared assertion body: row (l) replays row (b), so the two rows
// must be able to disagree ONLY about the oracle fields.
const hoverFlipAssertions = (r) => ({
  pairFound: Boolean(r.rowState.pair.from && r.rowState.pair.to),
  pairAdjacentInWorldOrder: Number.isInteger(r.rowState.pair.orderIndex),
  pairCoVisible:
    Boolean(r.rowState.pair.from?.screenPoint) && Boolean(r.rowState.pair.to?.screenPoint),
  pairSingleSlotSingleAddress:
    singleSlotSingleAddress(r.rowState.pair.from) &&
    singleSlotSingleAddress(r.rowState.pair.to),
  hoverOpensOnFrom: r.before.census.hover === r.rowState.pair.from?.id,
  hoverClosesOnTo: r.after.census.hover === r.rowState.pair.to?.id,
  attentionOpensOnFrom:
    r.rowState.attentionBefore?.from?.present === true &&
    r.rowState.attentionBefore?.to?.present === false,
  attentionClosesOnTo:
    r.rowState.attentionAfter?.from?.present === false &&
    r.rowState.attentionAfter?.to?.present === true,
  cacheHitsTwo: r.deltas.cache.hits === 2,
  hoverPaintRepacksZero: r.deltas.counters.paintRepacks.hoverPaint === 0,
  dirtyHoverZero: r.deltas.counters.dirty.hover === 0,
  slotTextWritesZero: r.deltas.counters.slotTextWrites === 0,
  geoZero: zeroTree(r.deltas.counters.geo),
  rafOne: r.deltas.counters.rafFrames === 1,
  storeFrameOne: r.deltas.counters.storeFrameExecs === 1,
  layoutExecsZero: sum(r.deltas.counters.layoutExecs) === 0,
  missesZero: r.deltas.cache.misses === 0,
  replacementsZero: r.deltas.cache.replacements === 0,
  sizeStable: r.deltas.cache.size === 0 && r.deltas.cache.addressCount === 0,
  conservation: cacheConservation(r),
  bound: boundAfter(r),
});

const moveToPairSide = (page, side) =>
  side?.screenPoint
    ? page.mouse.move(side.screenPoint.x, side.screenPoint.y)
    : Promise.resolve(null);

const readPairAttention = async (page, pair) => ({
  from: pair.from ? await readAttention(page, pair.from.id) : null,
  to: pair.to ? await readAttention(page, pair.to.id) : null,
});

const movePairAndCaptureAttention = async (page, state) => {
  state.attentionBefore = await readPairAttention(page, state.pair);
  await moveToPairSide(page, state.pair.to);
  state.attentionAfter = await readPairAttention(page, state.pair);
};

const waitForSlotTextWrites = async (page, minimum, timeoutMs = 60000) => {
  const deadline = Date.now() + timeoutMs;
  let counters = await readCounters(page);
  while (Date.now() < deadline && counters.slotTextWrites < minimum) {
    await sleep(50);
    counters = await readCounters(page);
  }
  if (counters.slotTextWrites < minimum) {
    throw new Error(
      `timed out waiting for slot-text-writes >= ${minimum}; observed ${counters.slotTextWrites}`,
    );
  }
  return counters;
};

// ===========================================================================
// ROW (h) OVERLAY-SLOT ROAD — §10 G4 row (h) AMENDED 2026-08-05 (Sid ruling,
// direct, no revalidation round).
//
// WHY THE RECUT.  Row (h)'s named candidate is "machine fold-toggle
// collapsing the machine's output slots".  It cannot remove a slot on this
// product: scene_runtime.cljs:398-400 builds `text-by-vi` from EVERY
// maintained store slot, so a slot leaves that map only when its vi leaves
// the store — `scene-rt/close-instance!`.  A fold changes a block's projected
// text; the block's slot survives (that is exactly what row (d) measures:
// slot-text-writes +1, geo lifecycle 1, geo destroyed 0).  V is therefore
// always 0 on the fold road, so the row as written is unperformable.
//
// THE RECUT (substance unchanged — §6 removal road 3, exercised WITHOUT truth
// death).  The halo overlay slot is a live TEXT slot: `render-halo!`
// (ground.cljs:735-791) carries its rows through `carry-ground-text-layouts!`
// under vi `:ground-halo`, so it owns H >= 1 layout addresses.  Both ends are
// literal UI drives on the product's own roads:
//   open  — contextmenu on proven-empty ground → events.cljs `>contextmenu`
//           → ground/handle-meta! → the `:halo/condense` floor row at
//           `:space/ground` (binding_material.cljc:448-450) → open-halo!
//   close — the Escape key → events.cljs:130 `{:type :escape}` →
//           keyboard.cljs:39 ground/escape! → render-halo! →
//           scene-rt/close-instance! :ground-halo
// NO truth changes: the context, every block, and every block slot survive
// the window untouched.  The destroy site then fires exactly once
// (runtime/render.cljs:124-129): geo destroyed +V, dirty :exit +V,
// evict-layouts-for-slot! removes the vanished slot's H addresses.
const rightClick = async (page, point) => {
  await page.mouse.move(point.x, point.y);
  await page.mouse.click(point.x, point.y, { button: "right" });
};

const openHaloSlot = async (page, point, timeoutMs = 10000) => {
  const before = (await profileState(page)).census;
  if (!point) {
    return { opened: false, slotsBefore: before.liveSlots, slotsAfter: before.liveSlots,
      addressesBefore: before.liveAddresses, addressesAfter: before.liveAddresses, halo: null };
  }
  await rightClick(page, point);
  const deadline = Date.now() + timeoutMs;
  let census = before;
  while (Date.now() < deadline) {
    census = (await profileState(page)).census;
    if (census.liveSlots > before.liveSlots) break;
    await sleep(50);
  }
  return {
    opened: census.liveSlots > before.liveSlots,
    slotsBefore: before.liveSlots,
    slotsAfter: census.liveSlots,
    addressesBefore: before.liveAddresses,
    addressesAfter: census.liveAddresses,
    halo: camelize(await page.evaluate(() => window.__ground.halo())),
  };
};

const nonHoverRows = () => [
  {
    name: "g4-a-same-glyph-text-edit",
    setup: async (page) => {
      const original = await page.evaluate((id) => window.__ground.truthText(id), ordinaryId);
      const sentinel = `q${original.slice(1)}`;
      await page.evaluate((id, text) => window.__ground.truthEdit(id, text), ordinaryId, sentinel);
      return { original, sentinel, receipt: { ordinaryId } };
    },
    action: (page, state) => page.evaluate(
      (id, text) => window.__ground.truthEdit(id, text),
      ordinaryId,
      `r${state.sentinel.slice(1)}`,
    ),
    restore: (page, state) => page.evaluate(
      (id, text) => window.__ground.truthEdit(id, text), ordinaryId, state.sentinel,
    ),
    assert: (r) => ({
      layoutOne: r.deltas.counters.layoutExecs.slotTextChange === 1,
      repackOne: r.deltas.counters.paintRepacks.slotTextChange === 1,
      inPlaceOne: r.deltas.counters.geo.inPlace === 1,
      writeOne: r.deltas.counters.slotTextWrites === 1,
      dirtyOne: r.deltas.counters.dirty.slotText === 1,
      missOne: r.deltas.cache.misses === 1,
      replacementOne: r.deltas.cache.replacements === 1,
      sizeStable: r.deltas.cache.size === 0 && r.deltas.cache.addressCount === 0,
      conservation: cacheConservation(r), bound: boundAfter(r),
      unnamedZero: unnamedZero(r, [
        "counters.layoutExecs.slotTextChange", "counters.paintRepacks.slotTextChange",
        "counters.geo.inPlace", "counters.slotTextWrites", "counters.dirty.slotText",
        "cache.misses", "cache.replacements",
      ]),
    }),
  },
  {
    // §10 G4 row (b) as AMENDED 2026-08-05 — see the pair-selection block
    // above for the forcing geometry and the exact recut.
    name: "g4-b-hover-flip",
    setup: async (page, _settled, consoleRows) => {
      // §9's literal reposition road: the corpus sits ~14,400 world units off
      // the boot camera, so nothing is hoverable until the product's own
      // wheel/drag input brings it on screen.  UNMEASURED — it closes before
      // the pre-state snapshot is taken.
      const segment = await repositionTo(page, ordinaryId, consoleRows);
      const pair = await findAdjacentCoVisiblePair(page);
      return {
        pair,
        repositionReached: Boolean(segment.targetPoint),
        receipt: hoverPairReceipt(
          pair,
          "§10 G4 row (b) AMENDED 2026-08-05 — pinned largest→ordinary is not " +
            "co-visible at any legal zoom; the flip runs between two adjacent " +
            "co-visible single-slot/single-address blocks chosen at run time.",
        ),
      };
    },
    action: (page, state) => movePairAndCaptureAttention(page, state),
    restore: (page, state) => moveToPairSide(page, state.pair.from),
    assert: (r) => ({
      repositionReached: r.rowState.repositionReached,
      ...hoverFlipAssertions(r),
      oracleFieldsZero:
        r.deltas.cache.oracleChecks === 0 &&
        r.deltas.cache.oracleMismatches === 0 &&
        r.deltas.counters.oracleLayoutExecs === 0,
      unnamedZero: unnamedZero(r),
    }),
  },
  {
    name: "g4-c-caret",
    setup: async (page) => {
      await page.evaluate((id) => window.__ground.caretMove(id, 0), ordinaryId);
      return { receipt: { ordinaryId } };
    },
    action: (page) => page.evaluate((id) => window.__ground.caretMove(id, 1), ordinaryId),
    restore: (page) => page.evaluate((id) => window.__ground.caretMove(id, 0), ordinaryId),
    assert: (r) => ({
      layoutZero: sum(r.deltas.counters.layoutExecs) === 0,
      geoZero: zeroTree(r.deltas.counters.geo), writesZero: r.deltas.counters.slotTextWrites === 0,
      missesZero: r.deltas.cache.misses === 0, dirtyZero: zeroTree(r.deltas.counters.dirty),
      repackBound: sum(r.deltas.counters.paintRepacks) <= 2,
      conservation: cacheConservation(r), bound: boundAfter(r),
      unnamedZero: unnamedZero(r, ["counters.paintRepacks.selection"]),
    }),
  },
  {
    name: "g4-c-text-selection",
    setup: async (page) => {
      await page.evaluate((id) => window.__ground.textSelectionMove(id, 0, 1), ordinaryId);
      return { receipt: { ordinaryId } };
    },
    action: (page) => page.evaluate(
      (id) => window.__ground.textSelectionMove(id, 0, 2), ordinaryId,
    ),
    restore: (page) => page.evaluate(
      (id) => window.__ground.textSelectionMove(id, 0, 1), ordinaryId,
    ),
    assert: (r) => ({
      layoutZero: sum(r.deltas.counters.layoutExecs) === 0,
      geoZero: zeroTree(r.deltas.counters.geo), writesZero: r.deltas.counters.slotTextWrites === 0,
      missesZero: r.deltas.cache.misses === 0, dirtyZero: zeroTree(r.deltas.counters.dirty),
      repackBound: sum(r.deltas.counters.paintRepacks) <= 2,
      conservation: cacheConservation(r), bound: boundAfter(r),
      unnamedZero: unnamedZero(r, ["counters.paintRepacks.selection"]),
    }),
  },
  {
    name: "g4-c-machine-selection",
    setup: async (page) => {
      const targets = camelize(await page.evaluate(() => window.__ground.profileTargets()));
      await page.evaluate(
        (id) => window.__ground.machineSelectionMove(id, 0, 0, 0, 1), targets.machine,
      );
      return { machine: targets.machine, receipt: { machine: targets.machine } };
    },
    action: (page, state) => page.evaluate(
      (id) => window.__ground.machineSelectionMove(id, 0, 0, 0, 2), state.machine,
    ),
    restore: (page, state) => page.evaluate(
      (id) => window.__ground.machineSelectionMove(id, 0, 0, 0, 1), state.machine,
    ),
    assert: (r) => ({
      targetPresent: Boolean(r.rowState.machine), layoutZero: sum(r.deltas.counters.layoutExecs) === 0,
      geoZero: zeroTree(r.deltas.counters.geo), writesZero: r.deltas.counters.slotTextWrites === 0,
      missesZero: r.deltas.cache.misses === 0, dirtyZero: zeroTree(r.deltas.counters.dirty),
      repackBound: sum(r.deltas.counters.paintRepacks) <= 2,
      conservation: cacheConservation(r), bound: boundAfter(r),
      unnamedZero: unnamedZero(r, ["counters.paintRepacks.selection"]),
    }),
  },
  ...["noise", "paste"].map((kind) => ({
    name: `g4-d-${kind}`,
    setup: async (page) => {
      const targets = camelize(await page.evaluate(() => window.__ground.profileTargets()));
      const id = kind === "noise" ? targets.machine : largestId;
      await page.evaluate(
        (target, rowKind) => rowKind === "noise"
          ? window.__ground.foldSet(target, "noise?", false)
          : window.__ground.pasteSet(target, false), id, kind,
      );
      return { id, receipt: { id, kind } };
    },
    action: (page, state) => page.evaluate(
      (target, rowKind) => rowKind === "noise"
        ? window.__ground.foldSet(target, "noise?", true)
        : window.__ground.pasteSet(target, true), state.id, kind,
    ),
    restore: (page, state) => page.evaluate(
      (target, rowKind) => rowKind === "noise"
        ? window.__ground.foldSet(target, "noise?", false)
        : window.__ground.pasteSet(target, false), state.id, kind,
    ),
    assert: (r) => ({
      targetPresent: Boolean(r.rowState.id), foldLayoutOne: r.deltas.counters.layoutExecs.foldProjection === 1,
      repackOne: r.deltas.counters.paintRepacks.foldProjection === 1,
      writeOne: r.deltas.counters.slotTextWrites === 1,
      geoOne: oneGeoWrite(r.deltas.counters.geo), dirtyOne: r.deltas.counters.dirty.slotText === 1,
      missOne: r.deltas.cache.misses === 1, replacementOne: r.deltas.cache.replacements === 1,
      sizeStable: r.deltas.cache.size === 0 && r.deltas.cache.addressCount === 0,
      conservation: cacheConservation(r), bound: boundAfter(r),
      noFreshOrDestroyed: r.deltas.counters.geo.fresh === 0 &&
        r.deltas.counters.geo.destroyed === 0,
      unnamedZero: unnamedZero(r, [
        "counters.layoutExecs.foldProjection", "counters.paintRepacks.foldProjection",
        "counters.geo.inPlace", "counters.geo.recloned", "counters.geo.capacityGrown",
        "counters.slotTextWrites", "counters.dirty.slotText",
        "cache.misses", "cache.replacements",
      ]),
    }),
  })),
  {
    name: "g4-e-backend",
    setup: async (page) => {
      const targets = camelize(await page.evaluate(() => window.__ground.profileTargets()));
      await page.evaluate(() => window.__ground.backendSet("msdf"));
      // AMENDED 2026-08-05 — PRE-READ of L, the live text-slot count, taken on
      // the settled MSDF pre-state and carried into the receipt: it is the
      // expected `slot-text-writes` value the row now NAMES.  Asserted equal
      // to the window-open census below, so the pre-read is falsifiable and
      // never a second, softer source of truth.
      const quiet = await waitForProfileQuiet(page);
      return {
        slots: quiet.state.census.liveTextSlots,
        receipt: {
          backend: targets.backend,
          preReadLiveTextSlots: quiet.state.census.liveTextSlots,
        },
      };
    },
    action: async (page, state) => {
      const before = await readCounters(page);
      await page.evaluate(() => window.__ground.backendSet("slug"));
      await waitForSlotTextWrites(page, before.slotTextWrites + state.slots);
    },
    restore: async (page, state) => {
      const before = await readCounters(page);
      await page.evaluate(() => window.__ground.backendSet("restore"));
      await waitForSlotTextWrites(page, before.slotTextWrites + state.slots);
    },
    assert: (r) => ({
      layoutZero: sum(r.deltas.counters.layoutExecs) === 0,
      digestEqual: r.before.cache.idDigest === r.after.cache.idDigest,
      repackAll: sum(r.deltas.counters.paintRepacks) === r.before.census.liveTextSlots,
      reclonedAll: r.deltas.counters.geo.recloned === r.before.census.liveTextSlots,
      destroyedPrior: r.deltas.counters.geo.destroyed === r.before.census.liveTextSlots,
      inPlaceZero: r.deltas.counters.geo.inPlace === 0,
      // §10 G4 row (e) AMENDED 2026-08-05 — Sid ruling (direct, no
      // revalidation round).  §590's definition stands and the counter is
      // correct: runtime/render.cljs:101 increments `slot-text-writes` once
      // per repacked slot at the same site that records the pack, so a
      // backend flip that reclones and reshapes every live text slot writes
      // exactly L of them.  NAMED with its exact value, never zeroed by
      // UNNAMED = 0.
      writesAll: r.deltas.counters.slotTextWrites === r.before.census.liveTextSlots,
      preReadMatchesWindowOpen: r.rowState.slots === r.before.census.liveTextSlots,
      dirtyOther: r.deltas.counters.dirty.other >= 1,
      providerTokenEqual: JSON.stringify(stable(r.before.targets.providerToken)) ===
        JSON.stringify(stable(r.after.targets.providerToken)),
      cacheCountersStable: r.deltas.cache.replacements === 0 &&
        r.deltas.cache.size === 0 && r.deltas.cache.addressCount === 0,
      conservation: cacheConservation(r), bound: boundAfter(r),
      // AMENDED 2026-08-05 — slot-text-writes is NAMED (= L) above and
      // therefore leaves the UNNAMED = 0 set.
      unnamedZero: unnamedZero(r, [
        "counters.paintRepacks.other", "counters.geo.recloned",
        "counters.geo.destroyed", "counters.dirty.other",
        "counters.slotTextWrites",
      ]),
    }),
  },
  {
    name: "g4-f-capacity",
    setup: async (page) => {
      const original = await page.evaluate((id) => window.__ground.truthText(id), ordinaryId);
      return { original, receipt: { ordinaryId } };
    },
    // §8's NAMED capacity-append road (ground.cljs profile-capacity-append!),
    // not a harness-composed truth string: the row's own hook appends the
    // 4,096-char suffix through the same truth road, so the receipt names the
    // drive it contracted for.  Restore rides `truthEdit` with the text
    // `truthText` returned before the window.
    action: (page) => page.evaluate(
      (id) => window.__ground.capacityAppend(id, 4096), ordinaryId,
    ),
    restore: (page, state) => page.evaluate(
      (id, text) => window.__ground.truthEdit(id, text), ordinaryId, state.original,
    ),
    assert: (r) => ({
      layoutOne: r.deltas.counters.layoutExecs.slotTextChange === 1,
      repackOne: r.deltas.counters.paintRepacks.slotTextChange === 1,
      capacityOne: r.deltas.counters.geo.capacityGrown === 1,
      otherGeoZero: r.deltas.counters.geo.fresh === 0 && r.deltas.counters.geo.inPlace === 0 &&
        r.deltas.counters.geo.recloned === 0 && r.deltas.counters.geo.destroyed === 0,
      writeOne: r.deltas.counters.slotTextWrites === 1,
      dirtyOne: r.deltas.counters.dirty.slotText === 1,
      missOne: r.deltas.cache.misses === 1, replacementOne: r.deltas.cache.replacements === 1,
      conservation: cacheConservation(r), bound: boundAfter(r),
      unnamedZero: unnamedZero(r, [
        "counters.layoutExecs.slotTextChange", "counters.paintRepacks.slotTextChange",
        "counters.geo.capacityGrown", "counters.slotTextWrites", "counters.dirty.slotText",
        "cache.misses", "cache.replacements",
      ]),
    }),
  },
  {
    name: "g4-g-truth-death",
    setup: async (page) => {
      const id = await page.evaluate(() => window.__ground.fixtureSet(true));
      const block = camelize(await page.evaluate((target) => window.__ground.profileBlock(target), id));
      return { id, owned: block?.ownedAddresses, slots: block?.slotCount,
        receipt: { id, owned: block?.ownedAddresses, slots: block?.slotCount } };
    },
    action: (page, state) => page.evaluate((id) => window.__ground.deleteBlock(id), state.id),
    restore: (page) => page.evaluate(() => window.__ground.fixtureSet(true)),
    assert: (r) => ({
      compositeFixture: r.rowState.owned >= 3,
      cacheEvicted: r.deltas.cache.size === -r.rowState.owned &&
        r.deltas.cache.addressCount === -r.rowState.owned,
      destroyed: r.deltas.counters.geo.destroyed === r.rowState.slots,
      exited: r.deltas.counters.dirty.exit === r.rowState.slots,
      layoutsZero: sum(r.deltas.counters.layoutExecs) === 0,
      missesZero: r.deltas.cache.misses === 0,
      conservation: cacheConservation(r), bound: boundAfter(r),
      unnamedZero: unnamedZero(r, [
        "counters.geo.destroyed", "counters.dirty.exit",
        "cache.size", "cache.addressCount",
      ]),
    }),
  },
  {
    // §10 G4 row (h) as AMENDED 2026-08-05 — see the overlay-slot block above
    // for why the fold candidate is unperformable and what replaced it.
    name: "g4-h-vanished-slot",
    setup: async (page) => {
      // Emptiness is decided by the PRODUCT's own pick: pointer-move!'s idle
      // branch and handle-meta!'s `pick-at` filter the identical address, so
      // `currentHover() === null` proves the contextmenu here resolves against
      // the space claim and can never address a block.
      const point = await findEmptyPoint(page, {
        x: viewport.width / 2,
        y: repositionInset,
      });
      const halo = await openHaloSlot(page, point);
      const state = await profileState(page);
      return {
        point,
        halo,
        // PRE-READ (in-receipt): the live slot/address census with the
        // overlay slot open.  V and H are read from these against the
        // post-window census, and `preBound`/`postBound` already pin
        // cache size = address count = live-address census on both edges,
        // so the address delta IS the vanished slot's owned addresses.
        slots: state.census.liveSlots,
        addresses: state.census.liveAddresses,
        receipt: {
          recut:
            "§10 G4 row (h) AMENDED 2026-08-05 — the named machine-fold " +
            "candidate cannot remove a slot (text-by-vi carries every " +
            "maintained slot); the vanishing road is the halo overlay slot: " +
            "contextmenu on empty ground opens it, Escape closes it, truth " +
            "untouched.",
          point,
          haloOpened: halo.opened,
          haloSubjectId: halo.halo?.subjectId ?? null,
          slotsBeforeOpen: halo.slotsBefore,
          slotsAfterOpen: halo.slotsAfter,
          addressesBeforeOpen: halo.addressesBefore,
          addressesAfterOpen: halo.addressesAfter,
          preReadSlots: state.census.liveSlots,
          preReadAddresses: state.census.liveAddresses,
        },
      };
    },
    action: (page) => page.keyboard.press("Escape"),
    restore: (page, state) => openHaloSlot(page, state.point),
    assert: (r) => {
      const vanishedSlots = -r.deltas.census.liveSlots;
      const vanishedAddresses = -r.deltas.census.liveAddresses;
      return {
        emptyGroundFound: Boolean(r.rowState.point),
        overlaySlotOpened: r.rowState.halo.opened,
        preReadMatchesWindowOpen:
          r.before.census.liveSlots === r.rowState.slots &&
          r.before.census.liveAddresses === r.rowState.addresses,
        slotVanished: vanishedSlots >= 1,
        addressesVanished: vanishedAddresses >= 1,
        truthSurvived: r.deltas.census.blocks === 0 &&
          r.deltas.census.servedChars === 0 &&
          r.deltas.census.machineBlocks === 0,
        destroyedV: r.deltas.counters.geo.destroyed === vanishedSlots,
        exitedV: r.deltas.counters.dirty.exit === vanishedSlots,
        cacheEvicted:
          r.deltas.cache.size === -vanishedAddresses &&
          r.deltas.cache.addressCount === -vanishedAddresses,
        layoutsZero: sum(r.deltas.counters.layoutExecs) === 0,
        missesZero: r.deltas.cache.misses === 0,
        replacementsZero: r.deltas.cache.replacements === 0,
        conservation: cacheConservation(r), bound: boundAfter(r),
        unnamedZero: unnamedZero(r, [
          "counters.geo.destroyed", "counters.dirty.exit",
          "cache.size", "cache.addressCount",
        ]),
      };
    },
  },
  {
    name: "g4-i-provider",
    setup: async (page) => {
      await page.evaluate(() => window.__ground.providerVariant(false));
      const state = await profileState(page);
      return { addresses: state.cache.addressCount, slots: state.census.liveTextSlots,
        receipt: { addresses: state.cache.addressCount, slots: state.census.liveTextSlots,
          provider: state.targets.providerToken } };
    },
    action: (page) => page.evaluate(() => window.__ground.providerVariant(true)),
    restore: (page) => page.evaluate(() => window.__ground.providerVariant(false)),
    assert: (r) => ({
      layoutsAll: r.deltas.counters.layoutExecs.providerChange === r.rowState.addresses,
      missesAll: r.deltas.cache.misses === r.rowState.addresses,
      digestChanged: r.before.cache.idDigest !== r.after.cache.idDigest,
      writesAll: r.deltas.counters.slotTextWrites === r.rowState.slots,
      repacksAll: r.deltas.counters.paintRepacks.providerChange === r.rowState.slots,
      geoAll: r.deltas.counters.geo.inPlace + r.deltas.counters.geo.recloned +
        r.deltas.counters.geo.capacityGrown === r.rowState.slots,
      freshDestroyedZero: r.deltas.counters.geo.fresh === 0 &&
        r.deltas.counters.geo.destroyed === 0,
      dirtyAll: r.deltas.counters.dirty.slotText === r.rowState.slots,
      replacementsAll: r.deltas.cache.replacements === r.rowState.addresses,
      sizeStable: r.deltas.cache.size === 0 && r.deltas.cache.addressCount === 0,
      conservation: cacheConservation(r), bound: boundAfter(r),
      unnamedZero: unnamedZero(r, [
        "counters.layoutExecs.providerChange", "counters.paintRepacks.providerChange",
        "counters.geo.inPlace", "counters.geo.recloned", "counters.geo.capacityGrown",
        "counters.slotTextWrites", "counters.dirty.slotText",
        "cache.misses", "cache.replacements",
      ]),
    }),
  },
  ...["binding", "contribution", "attention"].map((kind) => ({
    name: `g4-${kind === "binding" ? "j" : "k"}-${kind}`,
    setup: async (page) => {
      await page.evaluate((id, rowKind) => window.__ground.materialVariant(id, rowKind, false),
        ordinaryId, kind);
      const owned = await page.evaluate((id) => window.__ground.ownedAddresses(id), ordinaryId);
      return { owned, receipt: { ordinaryId, kind, owned } };
    },
    action: (page) => page.evaluate(
      (id, rowKind) => window.__ground.materialVariant(id, rowKind, true), ordinaryId, kind,
    ),
    restore: (page) => page.evaluate(
      (id, rowKind) => window.__ground.materialVariant(id, rowKind, false), ordinaryId, kind,
    ),
    assert: (r) => ({
      cacheHits: r.deltas.cache.hits === r.rowState.owned,
      layoutZero: sum(r.deltas.counters.layoutExecs) === 0,
      missesZero: r.deltas.cache.misses === 0,
      digestEqual: r.before.cache.idDigest === r.after.cache.idDigest,
      geoZero: zeroTree(r.deltas.counters.geo), writesZero: r.deltas.counters.slotTextWrites === 0,
      dirtyZero: zeroTree(r.deltas.counters.dirty),
      paintBound: sum(r.deltas.counters.paintRepacks) <= r.before.census.liveTextSlots,
      conservation: cacheConservation(r), bound: boundAfter(r),
      unnamedZero: unnamedZero(r, kind === "binding" ? [] : ["counters.paintRepacks.other"]),
    }),
  })),
  {
    // §10 G4 row (l) POSITIVE half — replays row (b) with the oracle enabled
    // inside EACH isolated trial.  The dedicated NEGATIVE half is the separate
    // one-trial `--case=oracle-negative` command and is NOT this row.
    // Carries row (b)'s 2026-08-05 amendment because it restates (b)'s flip.
    name: "g4-l-oracle-positive",
    setup: async (page, _settled, consoleRows) => {
      const segment = await repositionTo(page, ordinaryId, consoleRows);
      const pair = await findAdjacentCoVisiblePair(page);
      // Enabled LAST, so the oracle window opens on the same settled corpus
      // row (b) measures; the counter RESET that follows zeroes the oracle
      // fields too (text_layout.cljc layout-cache-reset-counters).
      await page.evaluate(() => window.__ground.enableLayoutOracle());
      return {
        pair,
        repositionReached: Boolean(segment.targetPoint),
        receipt: hoverPairReceipt(
          pair,
          "§10 G4 row (l) positive half — replays row (b) as AMENDED " +
            "2026-08-05 (adjacent co-visible pair, chosen at run time) with " +
            "the layout oracle enabled.",
        ),
      };
    },
    action: (page, state) => movePairAndCaptureAttention(page, state),
    restore: async (page, state) => {
      await moveToPairSide(page, state.pair.from);
      await page.evaluate(() => window.__ground.disableLayoutOracle());
    },
    assert: (r) => ({
      repositionReached: r.rowState.repositionReached,
      ...hoverFlipAssertions(r),
      oracleChecksTwo: r.deltas.cache.oracleChecks === 2,
      oracleLayoutExecsTwo: r.deltas.counters.oracleLayoutExecs === 2,
      oracleMismatchesZero: r.deltas.cache.oracleMismatches === 0,
      unnamedZero: unnamedZero(r, [
        "counters.oracleLayoutExecs", "cache.oracleChecks",
      ]),
    }),
  },
  {
    name: "g5-camera-pan",
    setup: async (page) => ({ receipt: { camera: await page.evaluate(() => window.__ground.camera()) } }),
    action: async (page) => {
      await page.mouse.move(20, 20); await page.mouse.down();
      // The first move crosses the product threshold and begins the continuous
      // verb; the second is the measured camera-pan continuation.
      await page.mouse.move(120, 120);
      await page.mouse.move(220, 220); await page.mouse.up();
    },
    restore: async (page) => {
      await page.mouse.move(220, 220); await page.mouse.down();
      await page.mouse.move(120, 120);
      await page.mouse.move(20, 20); await page.mouse.up();
    },
    assert: (r) => ({
      layoutZero: sum(r.deltas.counters.layoutExecs) === 0,
      writesZero: r.deltas.counters.slotTextWrites === 0,
      geoZero: zeroTree(r.deltas.counters.geo), repackZero: zeroTree(r.deltas.counters.paintRepacks),
      missesZero: r.deltas.cache.misses === 0, cameraDirty: r.deltas.counters.dirty.camera >= 1,
      conservation: cacheConservation(r), bound: boundAfter(r),
      unnamedZero: unnamedZero(r, ["counters.dirty.camera"]),
    }),
  },
  {
    name: "g5-camera-zoom",
    setup: async (page) => ({ receipt: { camera: await page.evaluate(() => window.__ground.camera()) } }),
    action: async (page) => {
      await page.mouse.move(400, 300);
      for (let i = 0; i < 4; i += 1) await page.mouse.wheel({ deltaY: -100 });
    },
    restore: async (page) => {
      for (let i = 0; i < 4; i += 1) await page.mouse.wheel({ deltaY: 100 });
    },
    assert: (r) => ({
      layoutZero: sum(r.deltas.counters.layoutExecs) === 0,
      writesZero: r.deltas.counters.slotTextWrites === 0,
      geoZero: zeroTree(r.deltas.counters.geo), repackZero: zeroTree(r.deltas.counters.paintRepacks),
      missesZero: r.deltas.cache.misses === 0, cameraDirty: r.deltas.counters.dirty.camera >= 1,
      conservation: cacheConservation(r), bound: boundAfter(r),
      unnamedZero: unnamedZero(r, ["counters.dirty.camera"]),
    }),
  },
  {
    name: "g5-order-only",
    setup: async (page) => {
      const targets = camelize(await page.evaluate(() => window.__ground.profileTargets()));
      return { pair: targets.orderPair, receipt: { pair: targets.orderPair } };
    },
    action: (page, state) => page.evaluate(
      ([a, b]) => window.__ground.swapOrder(a, b), state.pair,
    ),
    restore: (page, state) => page.evaluate(
      ([a, b]) => window.__ground.swapOrder(a, b), state.pair,
    ),
    assert: (r) => ({
      pairPresent: r.rowState.pair?.length === 2,
      truthOrderReversed:
        r.before.targets.orderPair?.[0] === r.rowState.pair?.[0] &&
        r.before.targets.orderPair?.[1] === r.rowState.pair?.[1] &&
        r.after.targets.orderPair?.[0] === r.rowState.pair?.[1] &&
        r.after.targets.orderPair?.[1] === r.rowState.pair?.[0],
      layoutZero: sum(r.deltas.counters.layoutExecs) === 0,
      writesZero: r.deltas.counters.slotTextWrites === 0,
      geoZero: zeroTree(r.deltas.counters.geo), repackZero: zeroTree(r.deltas.counters.paintRepacks),
      missesZero: r.deltas.cache.misses === 0,
      canonicalSceneOrderUnchanged: r.deltas.counters.dirty.order === 0,
      conservation: cacheConservation(r), bound: boundAfter(r),
      unnamedZero: unnamedZero(r),
    }),
  },
  {
    name: "g5-origin-shift-cascade",
    setup: async (page) => {
      await page.evaluate(() => window.__ground.originFixtureSet());
      await waitForProfileQuiet(page);
      const targets = camelize(await page.evaluate(() => window.__ground.profileTargets()));
      const candidate = targets.originShiftCandidates?.[0];
      if (candidate?.driver) {
        await page.evaluate(
          (id) => window.__ground.foldSet(id, "noise?", false), candidate.driver,
        );
        await waitForProfileQuiet(page);
      }
      return {
        candidate,
        receipt: {
          driver: candidate?.driver || null,
          observed: candidate?.observed || [],
          initialObservedY: candidate?.observedY || [],
        },
      };
    },
    action: (page, state) => page.evaluate(
      (id) => id ? window.__ground.foldSet(id, "noise?", true) : false,
      state.candidate?.driver,
    ),
    restore: (page, state) => page.evaluate(
      (id) => id ? window.__ground.foldSet(id, "noise?", false) : false,
      state.candidate?.driver,
    ),
    assert: (r) => {
      const beforeTarget = r.before.targets.originShiftCandidates?.find(
        (candidate) => candidate.driver === r.rowState.candidate?.driver,
      );
      const afterTarget = r.after.targets.originShiftCandidates?.find(
        (candidate) => candidate.driver === r.rowState.candidate?.driver,
      );
      const shifted = (beforeTarget?.observedY || []).filter(
        (y, index) => y !== afterTarget?.observedY?.[index],
      ).length;
      return {
        candidatePresent: Boolean(r.rowState.candidate?.driver),
        observedShifted: shifted >= 1,
        layoutOne: r.deltas.counters.layoutExecs.foldProjection === 1,
        repackOne: r.deltas.counters.paintRepacks.foldProjection === 1,
        writeOne: r.deltas.counters.slotTextWrites === 1,
        geoOne: oneGeoWrite(r.deltas.counters.geo),
        noFreshOrDestroyed: r.deltas.counters.geo.fresh === 0 &&
          r.deltas.counters.geo.destroyed === 0,
        dirtyOne: r.deltas.counters.dirty.slotText === 1,
        missOne: r.deltas.cache.misses === 1,
        replacementOne: r.deltas.cache.replacements === 1,
        sizeStable: r.deltas.cache.size === 0 && r.deltas.cache.addressCount === 0,
        conservation: cacheConservation(r), bound: boundAfter(r),
        unnamedZero: unnamedZero(r, [
          "counters.layoutExecs.foldProjection", "counters.paintRepacks.foldProjection",
          "counters.geo.inPlace", "counters.geo.recloned", "counters.geo.capacityGrown",
          "counters.slotTextWrites", "counters.dirty.slotText",
          "cache.misses", "cache.replacements",
        ]),
      };
    },
  },
];

// ===========================================================================
// CONTRACTED ROW REGISTRY — every §10 G4 row (a)…(l) and every G5 probe, BY
// NAME, mapped to its implementation or to null.
//
// FAIL-HONEST LAW: `probe/positive` can NEVER exit 0 while a contracted row
// has no implementation.  Each registry entry whose `impl` is null pushes a
// named red assertion `contractedRowUnimplemented:<name>` into the run's red
// set, so the mode exits 3 and the receipt states exactly WHICH contracted
// rows are missing — no silent "the matrix is complete" from a partial run.
// The converse is guarded too: an implemented row that is not registered
// pushes `unregisteredRowImplemented:<name>`.
//
// FOLLOW-UP PROTOCOL: a later engineer implements rows ONE AT A TIME — write
// the row object into `nonHoverRows()` under its registered name, and that
// entry's red assertion disappears by itself.  Nothing else changes; there is
// no count to bump and no separate list to keep in sync.
// ===========================================================================
const contractedRowRegistry = () => {
  const implemented = new Map(nonHoverRows().map((row) => [row.name, row]));
  const entry = (name, gate, contractRef, note = null) => ({
    name, gate, contractRef, note, impl: implemented.get(name) || null,
  });
  return [
    entry("g4-a-same-glyph-text-edit", "G4", "§10 G4 row (a)"),
    entry("g4-b-hover-flip", "G4", "§10 G4 row (b)",
      "AMENDED 2026-08-05 (Sid ruling, direct, no revalidation round) and " +
      "IMPLEMENTED. The pinned largest→ordinary flip needs the pinned pair " +
      "co-visible, which the geometry forbids at every legal zoom (8,142.95 " +
      "world units apart, zoom floor 0.1, 601px viewport) — the same defect " +
      "§9 was amended for. The row now flips between two ADJACENT CO-VISIBLE " +
      "single-slot/single-address blocks discovered from the live corpus at " +
      "run time through §9's literal reposition road. The flip proves +2 " +
      "cache hits and an attention-box handoff, with zero text layout, paint, " +
      "GPU writes, or geometry lifecycle changes. No synthetic pick/hover " +
      "mutation is used."),
    entry("g4-c-caret", "G4", "§10 G4 row (c) probe 1"),
    entry("g4-c-text-selection", "G4", "§10 G4 row (c) probe 2"),
    entry("g4-c-machine-selection", "G4", "§10 G4 row (c) probe 3"),
    entry("g4-d-noise", "G4", "§10 G4 row (d) probe 1 — machine fold"),
    entry("g4-d-paste", "G4", "§10 G4 row (d) probe 2 — paste flip"),
    entry("g4-e-backend", "G4", "§10 G4 row (e)"),
    entry("g4-f-capacity", "G4", "§10 G4 row (f)"),
    entry("g4-g-truth-death", "G4", "§10 G4 row (g)"),
    entry("g4-h-vanished-slot", "G4", "§10 G4 row (h)",
      "AMENDED 2026-08-05 (Sid ruling, direct, no revalidation round) and " +
      "IMPLEMENTED as an EXECUTED row — the declared ALTERNATIVE (source " +
      "trace + row (g)'s run) is no longer needed. The named machine-fold " +
      "candidate cannot remove a slot: scene_runtime.cljs builds `text-by-vi` " +
      "from EVERY maintained slot, so a slot vanishes only at " +
      "`close-instance!`, and a fold leaves the block's slot alive (row d " +
      "measures exactly that). The vanishing road is now the halo overlay " +
      "slot — contextmenu on proven-empty ground opens it, Escape closes it, " +
      "truth untouched — driving runtime/render.cljs' destroy site directly."),
    entry("g4-i-provider", "G4", "§10 G4 row (i)"),
    entry("g4-j-binding", "G4", "§10 G4 row (j)"),
    entry("g4-k-contribution", "G4", "§10 G4 row (k) probe 1"),
    entry("g4-k-attention", "G4", "§10 G4 row (k) probe 2"),
    entry("g4-l-oracle-positive", "G4", "§10 G4 row (l) positive half",
      "IMPLEMENTED: replays row (b) — as AMENDED 2026-08-05, i.e. over the " +
      "run-time adjacent co-visible pair — with the oracle enabled inside " +
      "EACH isolated positive trial (hits +2 · oracle-checks +2 · " +
      "oracle-layout-execs +2 · mismatches 0), disabled outside the window. " +
      "The dedicated NEGATIVE half is a separate one-trial command " +
      "(--case=oracle-negative) and is NOT this row."),
    entry("g5-camera-pan", "G5", "§10 G5 probe 1"),
    entry("g5-camera-zoom", "G5", "§10 G5 probe 2"),
    entry("g5-order-only", "G5", "§10 G5 probe 3"),
    entry("g5-origin-shift-cascade", "G5", "§10 G5 probe 4"),
  ];
};

const rowCoverage = (registry) => {
  const missing = registry.filter((row) => !row.impl).map((row) => row.name);
  const registered = new Set(registry.map((row) => row.name));
  const stray = nonHoverRows()
    .map((row) => row.name)
    .filter((name) => !registered.has(name));
  const reds = {};
  for (const name of missing) reds[`contractedRowUnimplemented:${name}`] = false;
  for (const name of stray) reds[`unregisteredRowImplemented:${name}`] = false;
  const counted = (gate, predicate) =>
    registry.filter((row) => row.gate === gate && predicate(row)).length;
  return {
    contracted: registry.length,
    implemented: registry.length - missing.length,
    implementedMeans: "row definitions present; not execution PASS",
    executionStatus: profilerSettlement.status,
    all20RowsPassed: false,
    retainedPositiveRunStoppedAt: profilerSettlement.retainedPositiveRunStoppedAt,
    missing,
    stray,
    g4Contracted: counted("G4", () => true),
    g4Implemented: counted("G4", (row) => Boolean(row.impl)),
    g5Contracted: counted("G5", () => true),
    g5Implemented: counted("G5", (row) => Boolean(row.impl)),
    reds,
    inventory: registry.map((row) => ({
      name: row.name,
      gate: row.gate,
      contractRef: row.contractRef,
      implemented: Boolean(row.impl),
      note: row.note,
    })),
  };
};

const terminalTruthTable = () => {
  const classify = ({ stops = [], assertionRed = false, environmentHole = false, wallRed = false }) => {
    const sortedStops = [...stops].sort();
    if (sortedStops.length > 0) {
      return `PACKAGE BLOCKED — {${sortedStops.join(",")}}`;
    }
    if (assertionRed) return "PACKAGE FAIL";
    if (environmentHole) return "UNCLASSIFIED (environment)";
    if (wallRed) {
      return "LINEAR-CORRECTION GREEN / EXPERIENCE-BAR RED — STEP-5 RULING REQUIRED";
    }
    return "PACKAGE PASS";
  };
  const rows = [
    [{ stops: ["S6"] }, "PACKAGE BLOCKED — {S6}"],
    [{ stops: ["S4", "S2"], assertionRed: true }, "PACKAGE BLOCKED — {S2,S4}"],
    [{ assertionRed: true }, "PACKAGE FAIL"],
    [{ assertionRed: true, environmentHole: true }, "PACKAGE FAIL"],
    [{ environmentHole: true }, "UNCLASSIFIED (environment)"],
    [
      { wallRed: true },
      "LINEAR-CORRECTION GREEN / EXPERIENCE-BAR RED — STEP-5 RULING REQUIRED",
    ],
    [{}, "PACKAGE PASS"],
  ];
  const results = rows.map(([input, expected]) => ({
    input,
    expected,
    actual: classify(input),
  }));
  return {
    pass: results.every((row) => row.actual === row.expected),
    rows: `${results.filter((row) => row.actual === row.expected).length}/7`,
    results,
  };
};

// §10 environment law AMENDED 2026-08-05 — the non-gating diagnostic matrix.
// Same rows, same paired-trial protocol, same count assertions; the results
// are STRUCTURALLY unable to reach the gate because they never appear under
// `measurements.assertions` (the only surface `main` reads for the red set).
const diagnosticMatrix = async (
  browser, invocation, consoleRows, registry, probeCase, eligibility,
) => {
  const base = {
    nonGatingDiagnostic: true,
    isGateInput: false,
    barVerdictIssued: false,
    barsEvaluated: false,
    whyNonGating:
      "fallback adapter (CPU rasterizer): counts are rasterizer-independent " +
      "and run diagnostically; bars, PASS and every green remain " +
      "founding-box-only. Exit stays 1 / unclassified-environment and the " +
      "terminal red set stays blank.",
    functionalEligibility: eligibility.functional,
    timingEligibility: eligibility.timing,
    failingEnvironmentFields: eligibility.timingOnlyFields,
  };
  if (!eligibility.functional) {
    return {
      ...base,
      executed: false,
      reason:
        "functional eligibility failed (attestation/Chromium/viewport/corpus) " +
        "— the matrix cannot run, exactly as before this amendment.",
    };
  }
  if (probeCase !== "positive") {
    return {
      ...base,
      executed: false,
      reason:
        "diagnostics run for the positive matrix only; the oracle-negative " +
        "command is a one-trial mutation-rejection proof whose contracted " +
        "terminal is exit 3 on the founding box.",
    };
  }
  const matrix = [];
  for (const row of registry) {
    if (!row.impl) continue;
    matrix.push(await runPairedRow(browser, invocation, consoleRows, row.impl));
  }
  const redsOf = (row) => [
    ...Object.entries(row.assertions).filter(([, pass]) => !pass).map(([name]) => name),
    ...row.trials.flatMap((trial, index) =>
      Object.entries(trial.assertions)
        .filter(([, pass]) => !pass)
        .map(([name]) => `trial${index + 1}:${name}`)),
  ];
  const rowOutcomes = matrix.map((row) => {
    const red = redsOf(row);
    return { name: row.name, green: red.length === 0, red };
  });
  return {
    ...base,
    executed: true,
    rowsExecuted: matrix.length,
    rowOutcomes,
    diagnosticRowsGreen: rowOutcomes.every((row) => row.green),
    matrix,
  };
};

const probeMeasurements = async (
  browser, page, settled, probeCase, invocation, consoleRows, eligibility,
) => {
  const terminalClassification = terminalTruthTable();
  const registry = contractedRowRegistry();
  const coverage = rowCoverage(registry);
  const inventory = coverage.inventory;
  // FAIL-HONEST: the named unimplemented-row reds ride EVERY positive return
  // path, so no early exit can produce a green positive run over a partial
  // matrix.  The oracle-negative case is exempt: its contracted red set is
  // exactly ["oracle-mismatch"] and it is not a positive matrix run.
  const coverageReds = probeCase === "positive" ? coverage.reds : {};
  const setupAssertions = {
    corpusSettled: !settled.timedOut,
    cacheBound: settled.live.cache.size === settled.live.cache.addressCount,
    liveAddressCensusBound:
      settled.live.cache.addressCount === settled.live.census.liveAddresses,
    ordinaryPresent: Boolean(settled.live.ordinary),
    largestPresent: Boolean(settled.live.largest),
    terminalClassification: terminalClassification.pass,
  };
  if (!Object.values(setupAssertions).every(Boolean)) {
    return {
      terminalClassification, inventory, rowCoverage: coverage, setupAssertions,
      assertions: { probeReady: false, ...coverageReds },
    };
  }
  if (!eligibility.timing) {
    // §10 environment law AMENDED 2026-08-05 — the GATE surface below is
    // byte-for-byte what it was before the amendment (gate did not run,
    // founding environment required, no paired trials), so classification,
    // exit code and the blanked terminal red set are unchanged.  What is NEW
    // is the separate diagnostic surface: when FUNCTIONAL eligibility holds,
    // the same rows execute and their count outcomes are recorded there.
    return {
      terminalClassification,
      inventory,
      rowCoverage: coverage,
      setupAssertions,
      assertions: {
        gateRan: false,
        foundingEnvironmentRequired: false,
        g4RowsAToLComplete: coverage.g4Implemented === coverage.g4Contracted,
        g5RowsComplete: coverage.g5Implemented === coverage.g5Contracted,
        pairedIsolatedTrialsComplete: false,
        ...coverageReds,
      },
      matrix: [],
      nonGatingDiagnostic: await diagnosticMatrix(
        browser, invocation, consoleRows, registry, probeCase, eligibility,
      ),
    };
  }

  const points = [settled.live.largest?.screenPoint, settled.live.ordinary?.screenPoint];
  const geometry = storyboardGeometry(settled.live.ordinary, settled.live.largest);

  if (probeCase === "positive") {
    const matrix = [];
    for (const row of registry) {
      if (!row.impl) continue;
      matrix.push(await runPairedRow(browser, invocation, consoleRows, row.impl));
    }
    const matrixRowsGreen = matrix.every((row) =>
      Object.values(row.assertions).every(Boolean));
    const gateRows = (gate) =>
      new Set(registry.filter((row) => row.gate === gate).map((row) => row.name));
    const g4Names = gateRows("G4");
    const g5Names = gateRows("G5");
    return {
      terminalClassification,
      inventory,
      rowCoverage: coverage,
      setupAssertions,
      // Recorded, not asserted: the storyboard is G3/G9's affair and runs
      // under `--mode=hover`, which since the 2026-08-05 §9 amendment drives
      // literal reposition segments instead of requiring co-visibility.
      storyboardGeometry: geometry,
      storyboardOwner: "--mode=hover (G3/G9); §9 AMENDED 2026-08-05",
      // Row (h) is an EXECUTED row since the 2026-08-05 amendment; the §10
      // ALTERNATIVE (source trace + row (g)'s run) is NOT claimed. This field
      // now records only WHICH product road drives the destroy site, so the
      // receipt never reads as a convergence-by-declaration.
      vanishedSlotRoad: {
        row: "g4-h-vanished-slot",
        ruling: "executed-row · §10 G4 row (h) AMENDED 2026-08-05",
        alternativeClaimed: false,
        openDrive: "contextmenu on proven-empty ground → :halo/condense → open-halo!",
        closeDrive: "Escape key → ground/escape! → render-halo! → close-instance! :ground-halo",
        destroySite: "runtime/render.cljs reconcile-slot-text-geos! vanished-vi loop",
        evictionOwner: "ground/evict-layouts-for-slot!",
      },
      assertions: {
        gateRan: true,
        probeReady: true,
        // The named per-row reds below are the load-bearing honesty: these
        // three roll-ups only summarise them.
        g4RowsAToLComplete: coverage.g4Implemented === coverage.g4Contracted,
        g5RowsComplete: coverage.g5Implemented === coverage.g5Contracted &&
          matrix.filter((row) => g5Names.has(row.name))
            .every((row) => Object.values(row.assertions).every(Boolean)),
        g4RowsGreen: matrix.filter((row) => g4Names.has(row.name))
          .every((row) => Object.values(row.assertions).every(Boolean)),
        pairedIsolatedTrialsComplete: matrix.length === coverage.implemented,
        matrixRowsGreen,
        ...coverageReds,
      },
      matrix,
    };
  }

  if (points.some((point) => !point)) {
    return {
      terminalClassification,
      inventory,
      setupAssertions,
      storyboardGeometry: geometry,
      assertions: {
        gateRan: true,
        probeReady: false,
        pinnedBlocksVisible: false,
        oracleMismatch: false,
      },
    };
  }

  await moveAndAwaitFrame(page, points[0], largestId, consoleRows);
  await page.evaluate(() => window.__ground.enableLayoutOracle());
  await page.evaluate(() => window.__ground.resetCounters());
  await page.evaluate((id) => window.__ground.seedStaleLayout(id), ordinaryId);
  const transition = await moveAndAwaitFrame(
    page, points[1], ordinaryId, consoleRows,
  );
  const counters = camelize(await page.evaluate(() => window.__ground.counters()));
  const cache = compactCache(camelize(
    await page.evaluate(() => window.__ground.layoutCache()),
  ));
  await page.evaluate(() => window.__ground.disableLayoutOracle());
  const assertions = {
    gateRan: true,
    probeReady: true,
    cacheHitsTwo: cache.hits === 2,
    oracleChecksTwo: cache.oracleChecks === 2,
    oracleLayoutExecsTwo: counters.oracleLayoutExecs === 2,
    productionLayoutExecsZero: sum(counters.layoutExecs) === 0,
    productionMissesZero: cache.misses === 0,
    oracleMismatch: cache.oracleMismatches === 1,
    hoverPaintRepacksZero: counters.paintRepacks.hoverPaint === 0,
    dirtyHoverZero: counters.dirty.hover === 0,
    slotTextWritesZero: counters.slotTextWrites === 0,
    geoZero: zeroTree(counters.geo),
    exactlyOneRaf:
      transition.rafDelta === 1 && transition.storeFrameDelta === 1 &&
      transition.g8Delta === 0,
    storeFrameBound: counters.storeFrameExecs <= counters.rafFrames,
    cacheBound: cache.size === cache.addressCount,
  };
  return {
    terminalClassification, inventory, setupAssertions, assertions,
    storyboardGeometry: geometry, transition, counters, cache,
  };
};

const writeArtifacts = (invocation, receipt) => {
  const label = invocation.mode === "probe" ? `${invocation.mode}-${invocation.case}` : invocation.mode;
  const outDir = path.join(repoRoot, "target/shaping-correction", label);
  fs.mkdirSync(outDir, { recursive: true });
  const receiptFile = path.join(outDir, "receipt.json");
  fs.writeFileSync(receiptFile, `${JSON.stringify(receipt, null, 2)}\n`);
  const manifest = {
    schemaVersion: 1,
    artifacts: [
      {
        path: path.relative(repoRoot, receiptFile),
        bytes: fs.statSync(receiptFile).size,
        sha256: sha256(fs.readFileSync(receiptFile)),
      },
    ],
  };
  const manifestFile = path.join(outDir, "manifest.json");
  fs.writeFileSync(manifestFile, `${JSON.stringify(manifest, null, 2)}\n`);
  return {
    receipt: path.relative(repoRoot, receiptFile),
    manifest: path.relative(repoRoot, manifestFile),
  };
};

// §10 environment law AMENDED 2026-08-05 — cold/hover half of the split.
// These two modes have no eligibility gate of their own: they always measure.
// On a timing-ineligible box their COUNT outcomes are still worth recording,
// but nothing they produce may read as a verdict — so the gate surfaces are
// emptied and every outcome moves under the non-gating diagnostic.  The wall
// bars are DROPPED, not relocated: no bar verdict of any kind is issued or
// recorded on a fallback adapter.  Raw per-act input→RAF times and the visual
// settle number stay in the receipt as data (`transitions`, `visualSettleMs`).
const demoteToNonGatingDiagnostic = (measurements, mode, eligibility) => {
  if (!measurements) return measurements;
  const {
    assertions = {}, setupAssertions = {}, wallBars: _droppedBars, ...rest
  } = measurements;
  const redOf = (surface) =>
    Object.entries(surface).filter(([, pass]) => !pass).map(([name]) => name);
  return {
    ...rest,
    setupAssertions: {},
    assertions: {},
    wallBars: {},
    nonGatingDiagnostic: {
      nonGatingDiagnostic: true,
      isGateInput: false,
      barVerdictIssued: false,
      barsEvaluated: false,
      whyNonGating:
        "fallback adapter (CPU rasterizer): counts are rasterizer-independent " +
        "and run diagnostically; bars, PASS and every green remain " +
        "founding-box-only. Exit stays 1 / unclassified-environment and the " +
        "terminal red set stays blank.",
      functionalEligibility: eligibility.functional,
      timingEligibility: eligibility.timing,
      failingEnvironmentFields: eligibility.timingOnlyFields,
      mode,
      executed: true,
      setupAssertions,
      assertions,
      diagnosticRed: [...redOf(setupAssertions), ...redOf(assertions)],
      diagnosticGreen:
        redOf(setupAssertions).length === 0 && redOf(assertions).length === 0,
    },
  };
};

const main = async () => {
  let invocation;
  try {
    invocation = parseInvocation(process.argv.slice(2));
  } catch (error) {
    console.error(`[SHAPING-PROFILE] ${error.message}`);
    process.exit(1);
  }

  const userDataDir = fs.mkdtempSync(path.join(os.tmpdir(), "softland-shaping-profile-"));
  let browser;
  let attestation = { isFallbackAdapter: null, description: "not attested" };
  let environment = null;
  let corpus = null;
  let measurements = null;
  let environmentChecks = null;
  let eligibility = null;
  let navigationFault = null;
  const consoleRows = [];
  const navigationStartMs = Date.now();
  try {
    browser = await puppeteer.launch({
      executablePath: chromeExecutable,
      headless: "new",
      userDataDir,
      ignoreHTTPSErrors: true,
      args: launchArgs,
    });
    const page = await browser.newPage();
    await page.setViewport(viewport);
    page.on("console", (message) =>
      consoleRows.push({ type: message.type(), text: message.text(), atMs: Date.now() }));
    page.on("pageerror", (error) =>
      consoleRows.push({ type: "pageerror", text: String(error), atMs: Date.now() }));
    const attestationUrl = new URL("/favicon.ico", invocation.url).href;
    await page.goto(attestationUrl, { waitUntil: "load", timeout: 30000 });
    attestation = await adapterAttestation(page);
    await page.goto(invocation.url, { waitUntil: "domcontentloaded", timeout: 30000 });
    environment = await browserEnvironment(browser, page, page.url());
    await page.evaluate(
      (ids) => {
        window.__shapingProfileIds = ids;
      },
      { ordinary: ordinaryId, largest: largestId },
    );
    await page.waitForFunction(
      () => window.__ground?.profileCensus && window.__ground?.profileIdentity,
      { timeout: 30000 },
    );
    const settled = await settle(page, navigationStartMs, consoleRows);
    const identity = camelize(await page.evaluate(() => window.__ground.profileIdentity()));
    corpus = corpusReceipt(settled.live, identity);
    environmentChecks = environmentAssertions(attestation, environment, corpus);
    // §10 environment law AMENDED 2026-08-05 — functional vs timing.
    eligibility = eligibilityOf(environmentChecks);
    if (invocation.mode === "cold") measurements = coldMeasurements(settled);
    if (invocation.mode === "hover") {
      measurements = await hoverMeasurements(page, settled, consoleRows);
    }
    if (invocation.mode === "probe") {
      measurements = await probeMeasurements(
        browser, page, settled, invocation.case, invocation, consoleRows,
        eligibility,
      );
    }
    if (!eligibility.timing && invocation.mode !== "probe") {
      measurements = demoteToNonGatingDiagnostic(
        measurements, invocation.mode, eligibility,
      );
    }
  } catch (error) {
    navigationFault = { message: error.message, stack: error.stack };
  } finally {
    if (browser) await browser.close();
    fs.rmSync(userDataDir, { recursive: true, force: true });
  }

  const environmentFault =
    Boolean(navigationFault) ||
    !environmentChecks ||
    !Object.values(environmentChecks).every(Boolean);
  const assertionEntries = [
    ...Object.entries(measurements?.setupAssertions || {}),
    ...Object.entries(measurements?.assertions || {}),
  ];
  const assertionRed = assertionEntries.filter(([, pass]) => !pass).map(([name]) => name);
  const wallEntries = Object.entries(measurements?.wallBars || {});
  const wallRed = wallEntries.filter(([, pass]) => !pass).map(([name]) => name);
  const expectedNegative =
    invocation.mode === "probe" &&
    invocation.case === "oracle-negative" &&
    assertionRed.length === 0 &&
    measurements?.cache?.oracleMismatches === 1;
  let exitCode;
  let classification;
  if (environmentFault) {
    exitCode = 1;
    classification = "unclassified-environment";
  } else if (expectedNegative) {
    exitCode = 3;
    classification = "expected-oracle-negative";
  } else if (assertionRed.length > 0) {
    exitCode = 3;
    classification = "product-assertion-red";
  } else if (wallRed.length > 0) {
    exitCode = 2;
    classification = "experience-bar-red";
  } else {
    exitCode = 0;
    classification = "green";
  }

  const receipt = {
    adapterAttestation: attestation,
    profilerSettlement,
    environment,
    invocation: { ...invocation, effectiveUrl: environment?.app?.effectiveUrl || invocation.url },
    environmentAssertions: environmentChecks,
    // §10 environment law AMENDED 2026-08-05 — which half of eligibility held.
    // `timing: false` means NO bar verdict exists in this receipt and every
    // count outcome under `measurements.nonGatingDiagnostic` is operator
    // information only: it is never a gate input and never a gate green.
    environmentEligibility: eligibility,
    corpus,
    measurements,
    terminal: {
      classification,
      exitCode,
      redSet: environmentFault
        ? []
        : expectedNegative
          ? ["oracle-mismatch"]
          : assertionRed,
      wallRed,
      navigationFault,
    },
    console: {
      pageErrors: consoleRows.filter((row) => row.type === "pageerror"),
      warningsAndErrors: consoleRows.filter((row) => ["warning", "error", "pageerror"].includes(row.type)),
      g8: consoleRows.filter((row) => row.text.startsWith("[SCENE-FACES/G8]")),
      raf: consoleRows.filter((row) => row.text.startsWith("[RAF]")),
    },
  };
  const artifacts = writeArtifacts(invocation, receipt);
  console.log(
    `[SHAPING-PROFILE] ${JSON.stringify({
      mode: invocation.mode,
      case: invocation.case || null,
      classification,
      exitCode,
      environment: environmentChecks,
      corpus,
      profilerSettlement,
      redSet: receipt.terminal.redSet,
      wallRed,
      artifacts,
    })}`,
  );
  process.exit(exitCode);
};

await main();
