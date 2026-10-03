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
//   type:<person>:<x>:<y>:<text>  click the text field at design coordinates (its hidden
//                             textarea takes focus), select all, and put <text> in its place
//                             (the rest of the step, colons and spaces included)
//   wait:<ms>                 wait
//   exec:<command>            run a shell command from the repository root while the
//                             pages stay open (an operator's act, e.g. python3 bin/inland
//                             forget carol); prints its lines that carry an :answer
//   follow:<path>             wait for the file at <path>, then run the steps it holds
//                             (one per line, same forms), on the same pages: a
//                             run can be steered after looking at its screenshots, and
//                             the sessions (and so the selections) stay the same. A file
//                             holding `end` closes the browser.
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

const steps = process.argv.slice(2);
while (steps.length) {
  const step = steps.shift();
  const [kind, who, a, b] = step.split(':');
  if (kind === 'end') break;
  if (kind === 'wait') { await new Promise((r) => setTimeout(r, Number(who))); continue; }
  if (kind === 'follow') {
    const file = step.slice('follow:'.length);
    console.log(`following: waiting for ${file}`);
    while (!fs.existsSync(file)) await new Promise((r) => setTimeout(r, 1000));
    steps.unshift(...fs.readFileSync(file, 'utf8').split('\n').map((l) => l.trim()).filter(Boolean));
    continue;
  }
  if (kind === 'exec') {
    // exec:<shell command>, run from the repository root while the pages stay open
    const { execSync } = await import('node:child_process');
    console.log(execSync(step.slice(5), { cwd: root, encoding: 'utf8' }).split('\n').filter((l) => l.includes(':answer')).join('\n'));
    continue;
  }
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
  } else if (kind === 'type') {
    const text = step.split(':').slice(4).join(':');
    const p = await canvasPoint(page, Number(a), Number(b));
    await page.mouse.click(p.x, p.y);
    await page.waitForTimeout(400);
    await page.keyboard.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A');
    await page.keyboard.insertText(text);
    await page.waitForTimeout(1500);
  } else if (kind === 'line') {
    // material-line: baseline y = 175 + i*24; its hit is [40 (y-17) 756 24]
    const p = await canvasPoint(page, 300, 175 + Number(a) * 24 - 6);
    await page.mouse.click(p.x, p.y);
    await page.waitForTimeout(2500);
  }
}
await browser.close();
