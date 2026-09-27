// The pointer's screen, driven in the Mac's own Chrome (WebGPU through Metal),
// for evidence: log a person in through the host's login page, point at the
// material through the canvas, and keep a screenshot of each step under
// ../runs/screens/. Makes no provider call. Needs the app on :8127
// (bin/inland serve) and playwright-core in the clone's node_modules
// (npm install --no-save playwright-core).
//
//   node src/proposal/pointer-screen-2026-09-27/tools/screen.mjs <step> ...
//
// Steps run in order, each on the person's own page (a second person gets a
// second browser context, so a second session in the store):
//   login:<person>            log in with the passphrase from .inland-runtime/passphrases.txt
//   shot:<person>:<name>      screenshot the person's page as screens/<name>.png
//   click:<person>:<x>:<y>    click the canvas at design coordinates (1440 × 960)
//   line:<person>:<i>         click the i-th visible line of the material view (0-based)
//   wait:<ms>                 wait
import { chromium } from 'playwright-core';
import fs from 'node:fs';
import path from 'node:path';
import url from 'node:url';

const here = path.dirname(url.fileURLToPath(import.meta.url));
const root = path.resolve(here, '../../../..');
const shots = path.resolve(here, '../runs/screens');
fs.mkdirSync(shots, { recursive: true });

const words = Object.fromEntries(
  fs.readFileSync(path.join(root, '.inland-runtime/passphrases.txt'), 'utf8')
    .split('\n').filter(Boolean).map((line) => line.trim().split(/\s+/)));

const browser = await chromium.launch({ channel: 'chrome', headless: false });
const pages = {};

async function pageOf(person) {
  if (!pages[person]) {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } });
    pages[person] = await context.newPage();
    pages[person].on('console', (m) => { if (m.type() === 'error') console.log(`[${person} console]`, m.text()); });
  }
  return pages[person];
}

async function canvasPoint(page, x, y) {
  const box = await page.locator('canvas').first().boundingBox();
  const scale = box.width / 1440;
  return { x: box.x + x * scale, y: box.y + y * scale };
}

for (const step of process.argv.slice(2)) {
  const [kind, who, a, b] = step.split(':');
  if (kind === 'wait') { await new Promise((r) => setTimeout(r, Number(who))); continue; }
  const page = await pageOf(who);
  if (kind === 'login') {
    await page.goto('http://localhost:8127/');
    await page.fill('input[name=name]', who);
    await page.fill('input[name=pass]', words[who]);
    await Promise.all([page.waitForNavigation(), page.click('button')]);
    await page.waitForSelector('canvas', { timeout: 60000 });
    await page.waitForTimeout(8000);
    console.log(`logged in: ${who} at ${page.url()}`);
  } else if (kind === 'shot') {
    const file = path.join(shots, `${a}.png`);
    await page.screenshot({ path: file });
    console.log(`screenshot: ${file}`);
  } else if (kind === 'click') {
    const p = await canvasPoint(page, Number(a), Number(b));
    await page.mouse.click(p.x, p.y);
    await page.waitForTimeout(2500);
  } else if (kind === 'line') {
    // material-line: baseline y = 175 + i*24; its hit is [40 (y-17) 756 24]
    const p = await canvasPoint(page, 300, 175 + Number(a) * 24 - 6);
    await page.mouse.click(p.x, p.y);
    await page.waitForTimeout(2500);
  }
}
await browser.close();
